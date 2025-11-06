package org.javaup.kafka.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.dto.VoucherOrderDto;
import org.javaup.entity.VoucherReconcileLog;
import org.javaup.enums.BusinessType;
import org.javaup.enums.LogType;
import org.javaup.enums.SeckillVoucherOrderOperate;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.kafka.redis.RedisVoucherData;
import org.javaup.message.MessageExtend;
import org.javaup.service.IVoucherOrderService;
import org.javaup.service.IVoucherReconcileLogService;
import org.javaup.toolkit.SnowflakeIdGenerator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

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
    private RedisVoucherData redisVoucherData;
    
    @Resource
    private IVoucherReconcileLogService voucherReconcileLogService;
    
    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;
    
    
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
            redisVoucherData.rollbackRedisVoucherData(
                    SeckillVoucherOrderOperate.YES,
                    traceId,
                    message.getMessageBody().getVoucherId(),
                    message.getMessageBody().getUserId(),
                    message.getMessageBody().getOrderId()
            );
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
        try {
            voucherOrderService.createVoucherOrderV2(voucherOrderDto);
        }catch (DuplicateKeyException e){
            long traceId = snowflakeIdGenerator.nextId();
            redisVoucherData.rollbackRedisVoucherData(
                    SeckillVoucherOrderOperate.NO,
                    traceId,
                    message.getMessageBody().getVoucherId(),
                    message.getMessageBody().getUserId(),
                    message.getMessageBody().getOrderId()
            );
        }
    }
    
    @Override
    protected void afterConsumeFailure(final MessageExtend<SeckillVoucherMessage> message, 
                                       final Throwable throwable) {
        super.afterConsumeFailure(message, throwable);
        long traceId = snowflakeIdGenerator.nextId();
        redisVoucherData.rollbackRedisVoucherData(
                SeckillVoucherOrderOperate.YES,
                traceId,
                message.getMessageBody().getVoucherId(),
                message.getMessageBody().getUserId(),
                message.getMessageBody().getOrderId()
        );
        // 对账日志：异常-消费失败
        try {
            String detail = throwable == null ? "consume failed" : ("consume failed: " + throwable.getMessage());
            saveReconcileLog(LogType.RESTORE,
                    BusinessType.FAIL.getCode(), 
                    detail, 
                    message
            );
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
                    message
            );
        } catch (Exception e) {
            log.warn("保存对账日志失败(消费成功)", e);
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
