package org.javaup.kafka.producer;

import org.javaup.AbstractProducerHandler;
import org.javaup.kafka.message.SeckillVoucherMessage;
import org.javaup.message.MessageExtend;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SeckillVoucherProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherMessage>> {
    
    public SeckillVoucherProducer(final KafkaTemplate<String,MessageExtend<SeckillVoucherMessage>> kafkaTemplate) {
        super(kafkaTemplate);
    }
}
