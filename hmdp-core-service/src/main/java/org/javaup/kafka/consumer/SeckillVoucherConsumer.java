package org.javaup.kafka.consumer;

import cn.hutool.core.collection.ListUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.RedisKeyManage;
import org.javaup.dto.VoucherOrderDto;
import org.javaup.enums.BaseCode;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.lua.SeckillVoucherRollBackOperate;
import org.javaup.message.MessageExtend;
import org.javaup.redis.RedisKeyBuild;
import org.javaup.service.IVoucherOrderService;
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
    
    
    public SeckillVoucherConsumer() {
        super(SeckillVoucherMessage.class);
    }
    
    @KafkaListener(topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_TOPIC})
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        consumeRaw(value, key, headers);
    }
    
    @Override
    protected Boolean beforeConsume(MessageExtend<SeckillVoucherMessage> message) {
        long producerTimeTimestamp = message.getProducerTime().getTime();
        long delayTime = System.currentTimeMillis() - producerTimeTimestamp;
        if (delayTime > MESSAGE_DELAY_TIME){
            log.info("消费到kafka的创建优惠券消息延迟时间大于了 {} 毫秒 此订单消息被丢弃 订单号 : {}",
                    delayTime,message.getMessageBody().getOrderId());
            rollbackRedisVoucherData(message);
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
        rollbackRedisVoucherData(message);
    }
    
    public void rollbackRedisVoucherData(MessageExtend<SeckillVoucherMessage> message) {
        try {
            // 回滚redis中的数据
            List<String> keys = ListUtil.of(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, message.getMessageBody().getVoucherId()).getRelKey(),
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_USER_TAG_KEY, message.getMessageBody().getVoucherId()).getRelKey()
            );
            String[] args = new String[3];
            args[0] = message.getMessageBody().getVoucherId().toString();
            args[1] = message.getMessageBody().getUserId().toString();
            args[2] = message.getMessageBody().getOrderId().toString();
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
}
