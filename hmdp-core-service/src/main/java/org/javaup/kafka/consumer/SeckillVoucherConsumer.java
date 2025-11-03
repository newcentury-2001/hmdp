package org.javaup.kafka.consumer;

import jakarta.annotation.Resource;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.dto.VoucherOrderDto;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.message.MessageExtend;
import org.javaup.service.IVoucherOrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import java.util.Map;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

@Component
public class SeckillVoucherConsumer extends AbstractConsumerHandler<SeckillVoucherMessage> {
    
    @Resource
    private IVoucherOrderService voucherOrderService;
    
    public SeckillVoucherConsumer(final Class<SeckillVoucherMessage> payloadType) {
        super(payloadType);
    }
    
    @KafkaListener(topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_TOPIC})
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        consumeRaw(value, key, headers);
    }
    
    @Override
    protected Boolean beforeConsume(final MessageExtend<SeckillVoucherMessage> message) {
        //TODO
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
}
