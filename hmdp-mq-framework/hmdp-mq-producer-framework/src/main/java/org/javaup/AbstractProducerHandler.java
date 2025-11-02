package org.javaup;


import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProducerHandler<MessageExtend> {

    private final KafkaTemplate<String, MessageExtend> kafkaTemplate;

    public final CompletableFuture<SendResult<String, MessageExtend>> sendMqMessage(String topic, MessageExtend message) {
        Assert.hasText(topic, "topic must not be blank");
        Assert.notNull(message, "message must not be null");

        return kafkaTemplate.send(topic, message).whenComplete((result, throwable) -> {
            if (throwable == null) {
                afterSendSuccess(result);
            } else {
                afterSendFailure(topic, message, throwable);
            }
        });
    }

    protected void afterSendSuccess(SendResult<String, MessageExtend> result) {
        log.info("kafka message send success, topic={}, partition={}, offset={}",
            result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }

    protected void afterSendFailure(String topic, MessageExtend message, Throwable throwable) {
        log.error("kafka message send failed, topic={}, message={}", topic, JSON.toJSON(message), throwable);
    }
}
