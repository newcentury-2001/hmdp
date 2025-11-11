package org.javaup.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import jakarta.annotation.Resource;
import org.javaup.core.RedisKeyManage;
import org.javaup.entity.SeckillVoucher;
import org.javaup.entity.VoucherOrder;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.enums.ReconciliationStatus;
import org.javaup.model.RedisTraceLogModel;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IReconciliationTaskService;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IVoucherOrderService;
import org.javaup.service.IVoucherReconcileLogService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: 对账执行 接口
 * @author: 阿星不是程序员
 **/
@Service
public class ReconciliationTaskServiceImpl implements IReconciliationTaskService {
    
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    @Resource
    private IVoucherOrderService voucherOrderService;
    
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    
    @Resource
    private RedisCache redisCache;
    
    @Override
    public void reconciliationTaskExecute() {
        List<SeckillVoucher> seckillVoucherList = seckillVoucherService.lambdaQuery().list();
        for (SeckillVoucher seckillVoucher : seckillVoucherList) {
            reconciliationTaskExecute(seckillVoucher.getVoucherId());
        }
        
        
    }
    
    public void reconciliationTaskExecute(Long voucherId){
        List<VoucherOrder> voucherOrderList = voucherOrderService.lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .le(VoucherOrder::getCreateTime, LocalDateTimeUtil.offset(LocalDateTimeUtil.now(), 2, ChronoUnit.MINUTES))
                .eq(VoucherOrder::getReconciliationStatus, ReconciliationStatus.PENDING.getCode())
                .orderByAsc(VoucherOrder::getCreateTime)
                .list();
        for (VoucherOrder voucherOrder : voucherOrderList) {
            List<VoucherReconcileLog> voucherReconcileLogList = voucherReconcileLogService.lambdaQuery()
                    .eq(VoucherReconcileLog::getOrderId, voucherOrder.getId())
                    .orderByAsc(VoucherReconcileLog::getCreateTime)
                    .list();
            if (voucherReconcileLogList == null || voucherReconcileLogList.isEmpty()) {
                // 未找到任何对账日志，标记为异常
                voucherOrderService.lambdaUpdate()
                        .set(VoucherOrder::getReconciliationStatus, ReconciliationStatus.ABNORMAL.getCode())
                        .eq(VoucherOrder::getId, voucherOrder.getId())
                        .update();
                continue;
            }
            
            Map<String, RedisTraceLogModel> redisTraceLogMap =
                    redisCache.getAllMapForHash(
                            RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId),
                            RedisTraceLogModel.class
                    );

            // Redis 中对账日志键
            RedisKeyBuild traceLogKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, voucherId);
            // Redis 中库存键
            RedisKeyBuild stockKey = RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId);

            // 预先计算 traceLogKey 的 TTL（若当前没有 TTL）
            Long ttlSeconds = redisCache.getExpire(traceLogKey, TimeUnit.SECONDS);
            if (ttlSeconds == null || ttlSeconds <= 0) {
                SeckillVoucher voucher = seckillVoucherService.lambdaQuery()
                        .eq(SeckillVoucher::getVoucherId, voucherId)
                        .one();
                // 默认一小时
                long computedTtl = 3600L; 
                if (voucher != null && voucher.getEndTime() != null) {
                    LocalDateTime now = LocalDateTimeUtil.now();
                    long secondsUntilEnd = Math.max(0L, Duration.between(now, voucher.getEndTime()).getSeconds());
                    computedTtl = Math.max(1L, secondsUntilEnd + Duration.ofDays(1).getSeconds());
                }
                ttlSeconds = computedTtl;
            }

            for (VoucherReconcileLog voucherReconcileLog : voucherReconcileLogList) {
                Long traceId = voucherReconcileLog.getTraceId();
                String traceIdStr = String.valueOf(traceId);
                RedisTraceLogModel existed = redisTraceLogMap.get(traceIdStr);

                // 1) 若 Redis 缺少该 trace 日志，则根据数据库日志补齐
                if (Objects.isNull(existed)) {
                    RedisTraceLogModel model = new RedisTraceLogModel();
                    model.setLogType(String.valueOf(voucherReconcileLog.getLogType()));
                    model.setTs(LocalDateTimeUtil.toEpochMilli(voucherReconcileLog.getCreateTime()));
                    model.setOrderId(String.valueOf(voucherReconcileLog.getOrderId()));
                    model.setTraceId(traceIdStr);
                    model.setUserId(String.valueOf(voucherReconcileLog.getUserId()));
                    model.setVoucherId(String.valueOf(voucherReconcileLog.getVoucherId()));
                    model.setBeforeQty(voucherReconcileLog.getBeforeQty());
                    model.setChangeQty(voucherReconcileLog.getChangeQty());
                    model.setAfterQty(voucherReconcileLog.getAfterQty());

                    // 写入 Redis Hash：field 为 traceId
                    redisCache.putHash(traceLogKey, traceIdStr, model);
                    // 若 trace 日志键无 TTL，则补上
                    Long currentTtl = redisCache.getExpire(traceLogKey, TimeUnit.SECONDS);
                    if (currentTtl == null || currentTtl <= 0) {
                        redisCache.expire(traceLogKey, ttlSeconds, TimeUnit.SECONDS);
                    }

                    // 不在此处修改库存，避免因数据库日志顺序与实际执行顺序不一致造成错误校正
                }
            }

            // 3) 本订单对账完成，标记为一致
            voucherOrderService.lambdaUpdate()
                    .set(VoucherOrder::getReconciliationStatus, ReconciliationStatus.CONSISTENT.getCode())
                    .eq(VoucherOrder::getId, voucherOrder.getId())
                    .update();
        }
    }
}
