package org.javaup.kafka.consumer;

import cn.hutool.core.collection.ListUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.VoucherOrderDto;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.enums.BaseCode;
import org.javaup.enums.BusinessType;
import org.javaup.enums.LogType;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.lua.SeckillVoucherRollBackOperate;
import org.javaup.message.MessageExtend;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IVoucherOrderService;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Slf4j
@Component
public class SeckillVoucherConsumer extends AbstractConsumerHandler<SeckillVoucherMessage> {
    
    public static Long MESSAGE_DELAY_TIME = 10000L;
    
    @Resource
    private IVoucherOrderService voucherOrderService;
    
    @Resource
    private SeckillVoucherRollBackOperate seckillVoucherRollBackOperate;
    
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    @Resource
    private RedisCache redisCache;
    
    
    public SeckillVoucherConsumer() {
        super(SeckillVoucherMessage.class);
    }
    
    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_TOPIC}
    )
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        consumeRaw(value, key, headers);
    }
    
    @Override
    protected Boolean beforeConsume(MessageExtend<SeckillVoucherMessage> message) {
        long producerTimeTimestamp = message.getProducerTime().getTime();
        long delayTime = System.currentTimeMillis() - producerTimeTimestamp;
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (delayTime > MESSAGE_DELAY_TIME){
            log.info("消费到kafka的创建优惠券消息延迟时间大于了 {} 毫秒 此订单消息被丢弃 订单号 : {}",
                    delayTime,message.getMessageBody().getOrderId());
            long traceId = snowflakeIdGenerator.nextId();
            rollbackRedisVoucherData(traceId,message);
            // 对账日志：异常-消息延迟丢弃
            try {
                saveReconcileLog(LogType.RESTORE, 
                        BusinessType.TIMEOUT.getCode(), 
                        "message delayed " + delayTime + "ms, rollback redis", 
                        message);
            } catch (Exception e) {
                log.warn("保存对账日志失败(延迟丢弃)", e);
            }
            return false;
        }
        return true;
    }
    
    @Override
    protected void doConsume(MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage messageBody = message.getMessageBody();
        VoucherOrderDto voucherOrderDto = new VoucherOrderDto();
        voucherOrderDto.setId(messageBody.getOrderId());
        voucherOrderDto.setUserId(messageBody.getUserId());
        voucherOrderDto.setVoucherId(messageBody.getVoucherId());
        voucherOrderDto.setMessageId(message.getUuid());
        voucherOrderService.createVoucherOrderV2(voucherOrderDto);
    }
    
    @Override
    protected void afterConsumeFailure(final MessageExtend<SeckillVoucherMessage> message, final Throwable throwable) {
        super.afterConsumeFailure(message, throwable);
        long traceId = snowflakeIdGenerator.nextId();
        rollbackRedisVoucherData(traceId,message);
        // 对账日志：异常-消费失败
        try {
            String detail = throwable == null ? "consume failed" : ("consume failed: " + throwable.getMessage());
            saveReconcileLog(LogType.RESTORE,
                    BusinessType.FAIL.getCode(), 
                    detail, 
                    message);
        } catch (Exception e) {
            log.warn("保存对账日志失败(消费失败)", e);
        }
    }
    
    @Override
    protected void afterConsumeSuccess(MessageExtend<SeckillVoucherMessage> message) {
        super.afterConsumeSuccess(message);
        // 对账日志：一致-消费成功
        try {
            saveReconcileLog(LogType.DEDUCT,
                    BusinessType.SUCCESS.getCode(), 
                    "order created", 
                    message);
        } catch (Exception e) {
            log.warn("保存对账日志失败(消费成功)", e);
        }
    }
    
    public void rollbackRedisVoucherData(Long traceId,
                                         MessageExtend<SeckillVoucherMessage> message) {
        try {
            // 回滚redis中的数据
            List<String> keys = ListUtil.of(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, message.getMessageBody().getVoucherId()).getRelKey(),
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, message.getMessageBody().getVoucherId()).getRelKey(),
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_TRACE_LOG_TAG_KEY, message.getMessageBody().getVoucherId()).getRelKey()
            );
            String[] args = new String[6];
            args[0] = message.getMessageBody().getVoucherId().toString();
            args[1] = message.getMessageBody().getUserId().toString();
            args[2] = message.getMessageBody().getOrderId().toString();
            args[3] = String.valueOf(traceId);
            args[4] = String.valueOf(LogType.RESTORE.getCode());
            args[5] = String.valueOf(600);
            Integer result = seckillVoucherRollBackOperate.execute(
                    keys,
                    args
            );
            if (!result.equals(BaseCode.SUCCESS.getCode())) {
                //TODO
                log.error("回滚失败");
            }
        }catch (Exception e){
            //TODO
            log.error("回滚失败",e);
        }
    }
    
    private void saveReconcileLog(LogType logType,
                                  Integer businessType, 
                                  String detail, 
                                  MessageExtend<SeckillVoucherMessage> message) {
        SeckillVoucherMessage body = message.getMessageBody();
        VoucherReconcileLog logEntity = new VoucherReconcileLog();
        logEntity.setId(snowflakeIdGenerator.nextId())
                .setOrderId(body.getOrderId())
                .setUserId(body.getUserId())
                .setVoucherId(body.getVoucherId())
                .setMessageId(message.getUuid())
                .setBusinessType(businessType)
                .setDetail(detail)
                .setTraceId(body.getTraceId())
                .setLogType(logType.getCode())
                .setCreateTime(java.time.LocalDateTime.now())
                .setUpdateTime(java.time.LocalDateTime.now())
                .setBeforeQty(body.getBeforeQty())
                .setChangeQty(body.getChangeQty())
                .setAfterQty(body.getAfterQty())
                .setTraceId(body.getTraceId());
        if (logType == LogType.RESTORE) {
            logEntity.setBeforeQty(body.getAfterQty());
            logEntity.setAfterQty(body.getBeforeQty());
        }
        voucherReconcileLogService.save(logEntity);
    }
}
