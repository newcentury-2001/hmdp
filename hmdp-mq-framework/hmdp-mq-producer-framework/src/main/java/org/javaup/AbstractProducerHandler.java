package org.javaup;


import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaup.message.MessageExtend;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.Assert;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractProducerHandler<M extends MessageExtend<?>> {

    private final KafkaTemplate<String, M> kafkaTemplate;

    public final CompletableFuture<SendResult<String, M>> sendMqMessage(String topic, M message) {
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

    /**
     * 直接传入payload，内部自动包装为MessageExtend并发送
     */
    @SuppressWarnings("unchecked")
    public final <T> CompletableFuture<SendResult<String, M>> sendPayload(String topic, T payload) {
        M message = (M) MessageExtend.of(payload);
        return sendMqMessage(topic, message);
    }

    /**
     * 支持指定key（用于分区与幂等），并附带headers发送
     */
    @SuppressWarnings("unchecked")
    public final <T> CompletableFuture<SendResult<String, M>> sendPayload(String topic, String key, T payload, Map<String, String> headers) {
        M message = (M) MessageExtend.of(payload, key, headers);
        return sendRecord(topic, message);
    }

    /**
     * 基于ProducerRecord发送，映射MessageExtend中的key与headers到Kafka Headers
     */
    public final CompletableFuture<SendResult<String, M>> sendRecord(String topic, M message) {
        Assert.hasText(topic, "topic must not be blank");
        Assert.notNull(message, "message must not be null");

        ProducerRecord<String, M> record = new ProducerRecord<>(topic, message.getKey(), message);
        Map<String, String> headers = message.getHeaders();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((k, v) -> {
                if (Objects.nonNull(k) && Objects.nonNull(v)) {
                    record.headers().add(new RecordHeader(k, v.getBytes(StandardCharsets.UTF_8)));
                }
            });
        }

        return kafkaTemplate.send(record).whenComplete((result, throwable) -> {
            if (throwable == null) {
                afterSendSuccess(result);
            } else {
                afterSendFailure(topic, message, throwable);
            }
        });
    }

    /**
     * 批量发送：按顺序发送并等待所有Future完成（不保证原子性）
     */
    @SuppressWarnings("unchecked")
    public final <T> CompletableFuture<Void> sendBatch(String topic, List<T> payloads) {
        Assert.hasText(topic, "topic must not be blank");
        Assert.notNull(payloads, "payloads must not be null");
        CompletableFuture<?>[] futures = payloads.stream()
                .map(p -> (M) MessageExtend.of(p))
                .map(m -> sendMqMessage(topic, m))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * 阻塞发送：调用方需要同步等待结果时使用
     */
    public final <T> SendResult<String, M> sendAndWait(String topic, T payload) throws ExecutionException, InterruptedException {
        return sendPayload(topic, payload).get();
    }

    /**
     * 发送到死信队列（DLQ），topic采用约定：原topic + ".DLQ"
     */
    @SuppressWarnings("unchecked")
    public final <T> CompletableFuture<SendResult<String, M>> sendToDlq(String originalTopic, T payload, String reason) {
        String dlqTopic = originalTopic + ".DLQ";
        M message = (M) MessageExtend.of(payload);
        message.setHeaders(Map.of("dlqReason", reason));
        return sendRecord(dlqTopic, message);
    }

    protected void afterSendSuccess(SendResult<String, M> result) {
        log.info("kafka message send success, topic={}, partition={}, offset={}",
            result.getRecordMetadata().topic(), result.getRecordMetadata().partition(),
            result.getRecordMetadata().offset());
    }

    protected void afterSendFailure(String topic, M message, Throwable throwable) {
        log.error("kafka message send failed, topic={}, message={}", topic, JSON.toJSON(message), throwable);
    }
}
