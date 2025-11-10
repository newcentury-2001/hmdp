package org.javaup.kafka.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.RedisKeyManage;
import org.javaup.enums.BusinessType;
import org.javaup.enums.LogType;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.kafka.redis.RedisVoucherData;
import org.javaup.message.MessageExtend;
import org.javaup.model.SeckillVoucherFullModel;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.ISeckillVoucherService;
import org.javaup.service.IVoucherOrderService;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;


/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: Kafka 消费者：处理秒杀券下单消息。
 * 负责：
 * 1) 延迟过滤：超过阈值的消息丢弃并回滚；
 * 2) 正常消费：创建订单，幂等冲突时执行回滚；
 * 3) 失败处理：消费异常时回滚并记录对账日志；
 * 4) 成功处理：消费成功记录一致性对账日志。
 * @author: 阿星不是程序员
 **/

@Slf4j
@Component
public class SeckillVoucherConsumer extends AbstractConsumerHandler<SeckillVoucherMessage> {
    
    /**
     * 消息延迟阈值（毫秒），超过阈值则丢弃并回滚
     * */
    public static Long MESSAGE_DELAY_TIME = 10000L;
    
    /**
     * 订单服务：负责创建秒杀订单
     * */
    @Resource
    private IVoucherOrderService voucherOrderService;
    
    /**
     * Redis 回滚封装组件：包含 Lua 调用、指数退避重试与失败日志
     * */
    @Resource
    private RedisVoucherData redisVoucherData;

    /**
     * Redis缓存：用于统计店铺每日Top买家
     */
    @Resource
    private RedisCache redisCache;

    /**
     * 秒杀券服务：查询券详情以获取shopId
     */
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    
    /**
     * 对账日志服务：记录消费成功/失败等业务一致性日志
     * */
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    
    /**
     * 雪花算法：生成贯穿回滚/日志的 traceId
     * */
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    
    public SeckillVoucherConsumer() {
        super(SeckillVoucherMessage.class);
    }
    
    /**
     * Kafka 消息入口：委托框架转换并进入统一消费流程。
     */
    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_TOPIC}
    )
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        consumeRaw(value, key, headers);
    }
    
   
    /**
     * 消费前置过滤：若消息延迟超过阈值则丢弃并回滚，同时记录对账日志。
     * 返回 true 继续消费；返回 false 中断后续消费流程。
     */
    @Override
    protected Boolean beforeConsume(MessageExtend<SeckillVoucherMessage> message) {
        long producerTimeTimestamp = message.getProducerTime().getTime();
        long delayTime = System.currentTimeMillis() - producerTimeTimestamp;
        try {
            //延长点时间，用来方便展示前端等待效果
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (delayTime > MESSAGE_DELAY_TIME){
            log.info("消费到kafka的创建优惠券消息延迟时间大于了 {} 毫秒 此订单消息被丢弃 订单号 : {}",
                    delayTime,message.getMessageBody().getOrderId());
            long traceId = snowflakeIdGenerator.nextId();
            redisVoucherData.rollbackRedisVoucherData(
                    SeckillVoucherOrderOperate.YES,
                    traceId,
                    message.getMessageBody().getVoucherId(),
                    message.getMessageBody().getUserId(),
                    message.getMessageBody().getOrderId()
            );
            // 对账日志：异常-消息延迟丢弃
            try {
                voucherReconcileLogService.saveReconcileLog(LogType.RESTORE, 
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
    
    
    /**
     * 核心消费：尝试创建订单，若出现幂等冲突(DuplicateKeyException)则执行回滚。
     */
    @Override
    protected void doConsume(MessageExtend<SeckillVoucherMessage> message) {
        voucherOrderService.createVoucherOrderV2(message);
    }
    
    /**
     * 成功后处理：统计用户购买
     */
    @Override
    protected void afterConsumeSuccess(MessageExtend<SeckillVoucherMessage> message) {
        super.afterConsumeSuccess(message);
        // 统计“店铺每日Top买家”：将用户加入对应店铺当日ZSET并自增分数
        try {
            Long voucherId = message.getMessageBody().getVoucherId();
            Long userId = message.getMessageBody().getUserId();
            SeckillVoucherFullModel voucherFull = seckillVoucherService.queryByVoucherId(voucherId);
            if (Objects.isNull(voucherFull)) {
                return;
            }
            Long shopId = voucherFull.getShopId();
            // yyyyMMdd
            String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); 
            RedisKeyBuild dailyKey = RedisKeyBuild.createRedisKey(
                    RedisKeyManage.SECKILL_SHOP_TOP_BUYERS_DAILY_TAG_KEY,
                    shopId,
                    day
            );
            // 自增当日购买次数
            redisCache.incrementScoreForSortedSet(dailyKey, String.valueOf(userId), 1.0);
            // 若首次写入或无TTL，则设置保留时长（默认保留90天）
            Long ttl = redisCache.getExpire(dailyKey, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                redisCache.expire(dailyKey, 90, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("统计店铺Top买家失败，忽略不影响主流程", e);
        }
    }
    
    /**
     * 失败后处理：消费异常时回滚 Redis 并记录对账日志（异常）。
     */
    @Override
    protected void afterConsumeFailure(final MessageExtend<SeckillVoucherMessage> message, 
                                       final Throwable throwable) {
        super.afterConsumeFailure(message, throwable);
        SeckillVoucherOrderOperate seckillVoucherOrderOperate = SeckillVoucherOrderOperate.YES;
        if (throwable instanceof DuplicateKeyException) {
            seckillVoucherOrderOperate = SeckillVoucherOrderOperate.NO;
        }
        long traceId = snowflakeIdGenerator.nextId();
        redisVoucherData.rollbackRedisVoucherData(
                seckillVoucherOrderOperate,
                traceId,
                message.getMessageBody().getVoucherId(),
                message.getMessageBody().getUserId(),
                message.getMessageBody().getOrderId()
        );
        // 对账日志：异常-消费失败
        try {
            String detail = throwable == null ? "consume failed" : ("consume failed: " + throwable.getMessage());
            voucherReconcileLogService.saveReconcileLog(LogType.RESTORE,
                    BusinessType.FAIL.getCode(), 
                    detail, 
                    message
            );
        } catch (Exception e) {
            log.warn("保存对账日志失败(消费失败)", e);
        }
    }
}
