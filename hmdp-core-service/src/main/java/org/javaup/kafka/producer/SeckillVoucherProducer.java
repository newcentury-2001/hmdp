package org.javaup.kafka.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.AbstractProducerHandler;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.kafka.redis.RedisVoucherData;
import org.javaup.lua.SeckillVoucherRollBackOperate;
import org.javaup.message.MessageExtend;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/**
 * Kafka 生产者：发送秒杀券订单消息。
 * 特性：
 * - 在消息发送失败时，通过回调生成 traceId 并触发 Redis 回滚，
 *   保证库存与用户集合在失败场景下及时恢复一致性。
 */
@Slf4j
@Component
public class SeckillVoucherProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherMessage>> {
    
    /**
     * 生成全局唯一 traceId，用于串联失败回滚
     * */
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    /**
     * 回滚 Lua 脚本执行器（由 RedisVoucherData 使用）
     * */
    @Resource
    private SeckillVoucherRollBackOperate seckillVoucherRollBackOperate;
    
    /**
     * 对账日志服务（主记录在消费者侧，此处保留注入）
     * */
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    
    /**
     * Redis 回滚封装组件：包含 Lua 调用、指数退避重试与失败日志
     * */
    @Resource
    private RedisVoucherData redisVoucherData;
    
    public SeckillVoucherProducer(final KafkaTemplate<String,MessageExtend<SeckillVoucherMessage>> kafkaTemplate) {
        super(kafkaTemplate);
    }
    
    
    /**
     * 发送失败回调：生成 traceId 并触发 Redis 侧回滚。
     * 目的：保证发送失败时，库存与用户集合及时恢复一致性。
     */
    @Override
    protected void afterSendFailure(final String topic, final MessageExtend<SeckillVoucherMessage> message, final Throwable throwable) {
        super.afterSendFailure(topic, message, throwable);
        // 生成贯穿回滚流程的 traceId
        long traceId = snowflakeIdGenerator.nextId();
        // 调用 RedisVoucherData 执行回滚（YES 标识执行订单恢复）
        redisVoucherData.rollbackRedisVoucherData(
                SeckillVoucherOrderOperate.YES,
                traceId,
                message.getMessageBody().getVoucherId(),
                message.getMessageBody().getUserId(),
                message.getMessageBody().getOrderId());
    }
}
