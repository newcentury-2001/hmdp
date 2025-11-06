package org.javaup.kafka.producer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.AbstractProducerHandler;
import org.javaup.kafka.message.SeckillVoucherInvalidationMessage;
import org.javaup.message.MessageExtend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Kafka 生产者：广播“秒杀券缓存失效”消息到所有实例。
 * 负责：
 * 1) 发送失败时结构化日志与指标记录；
 * 2) 退避重试（通过 Header 记录重试次数，避免无限递归）；
 * 3) 超过重试阈值后，投递到 DLQ 供后续补偿；
 * 4) 成功后记录成功指标，若为 DLQ 重放则额外审计日志。
 */
@Slf4j
@Component
public class SeckillVoucherInvalidationProducer extends AbstractProducerHandler<MessageExtend<SeckillVoucherInvalidationMessage>> {

    /**
     * Header 中标记的重试次数键名
     * */
    private final static String RETRY_COUNT = "retryCount";
    
    public SeckillVoucherInvalidationProducer(final KafkaTemplate<String, MessageExtend<SeckillVoucherInvalidationMessage>> kafkaTemplate) {
        super(kafkaTemplate);
    }
    /**
     * Micrometer 指标注册器，用于上报成功/失败/重试等指标
     * */
    @Resource
    private MeterRegistry meterRegistry;

    /**
     * 最大重试次数，超出则转交 DLQ
     * */
    @Value("${seckill.cache.invalidate.retry.maxAttempts:3}")
    private int retryMaxAttempts;

    /**
     * 初始退避毫秒
     * */
    @Value("${seckill.cache.invalidate.retry.initialBackoffMillis:200}")
    private long initialBackoffMillis;

    /**
     * 最大退避毫秒
     * */
    @Value("${seckill.cache.invalidate.retry.maxBackoffMillis:800}")
    private long maxBackoffMillis;

    /**
     * 审计日志记录器：记录关键操作（如 DLQ 投递/重放成功）
     * */
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    
    
    /**
     * 发送失败处理：结构化日志、指标、自适应退避重试；超限后转交 DLQ。
     */
    @Override
    protected void afterSendFailure(final String topic, final MessageExtend<SeckillVoucherInvalidationMessage> message, final Throwable throwable) {
        // 1) 结构化日志，便于定位问题
        final SeckillVoucherInvalidationMessage body = message.getMessageBody();
        final Long voucherId = body.getVoucherId();
        final String reason = body.getReason();
        final String errMsg = throwable == null ? "unknown" : throwable.getMessage();
        log.error("SeckillVoucherInvalidation send failed, topic={}, uuid={}, key={}, voucherId={}, reason={}, error= {}",
                topic, message.getUuid(), message.getKey(), voucherId, reason, errMsg, throwable);

        // 指标：失败计数
        safeInc("seckill_invalidation_send_failures", "topic", topic);

        // 2) 失败重试（可配置次数 + 退避），通过 header 标记重试次数避免无限递归
        Map<String, String> headers = message.getHeaders();
        headers = headers == null ? new HashMap<>(8) : new HashMap<>(headers);
        int retryCount = 0;
        try {
            if (headers.containsKey(RETRY_COUNT)) {
                retryCount = Integer.parseInt(headers.get(RETRY_COUNT));
            }
        } catch (Exception ignore) {
        }

        if (retryCount < retryMaxAttempts) {
            long backoff = Math.min(initialBackoffMillis * (1L << retryCount), maxBackoffMillis);
            headers.put(RETRY_COUNT, String.valueOf(retryCount + 1));
            headers.put("lastError", truncate(errMsg));
            message.setHeaders(headers);
            log.warn("Retry sending cache invalidation, topic={}, uuid={}, voucherId={}, retryCount={}, backoffMs={}",
                    topic, message.getUuid(), voucherId, retryCount + 1, backoff);
            safeInc("seckill_invalidation_send_retries", "topic", topic);
            // 简单退避等待后重试
            sleepQuietly(backoff);
            // 异步重试：若再失败会再次进入本方法，直到超过最大重试次数
            sendRecord(topic, message);
            return;
        }

        // 3) 放入 DLQ，便于后续人工/自动补偿
        final String dlqReason = "send_invalid_cache_broadcast_failed: " + truncate(errMsg);
        try {
            sendToDlq(topic, body, dlqReason);
            log.warn("Send cache invalidation to DLQ, originalTopic={}, uuid={}, voucherId={}, dlqReason={}",
                    topic, message.getUuid(), voucherId, dlqReason);
            auditLog.warn("DLQ_PUBLISH|topic={}|uuid={}|key={}|voucherId={}|reason={}",
                    topic, message.getUuid(), message.getKey(), voucherId, dlqReason);
            safeInc("seckill_invalidation_send_dlq", "topic", topic);
        } catch (Exception e) {
            log.error("Send cache invalidation to DLQ failed, originalTopic={}, uuid={}, voucherId={}, error={}",
                    topic, message.getUuid(), voucherId, e.getMessage(), e);
            safeInc("seckill_invalidation_send_dlq_failures", "topic", topic);
        }
    }
    
    /**
     * 发送成功处理：上报成功指标；若为 DLQ 重放则记录审计日志与指标。
     */
    @Override
    protected void afterSendSuccess(SendResult<String, MessageExtend<SeckillVoucherInvalidationMessage>> result) {
        super.afterSendSuccess(result);
        String topic = result.getRecordMetadata().topic();
        MessageExtend<SeckillVoucherInvalidationMessage> message = result.getProducerRecord().value();
        boolean dlqReplay = message != null && message.getHeaders() != null && "1".equals(message.getHeaders().getOrDefault("dlqReplayCount", "0"));
        safeInc("seckill_invalidation_send_success", "topic", topic);
        if (dlqReplay) {
            safeInc("seckill_invalidation_dlq_replay_success", "topic", topic);
            auditLog.info("DLQ_REPLAY_SUCCESS|topic={}|uuid={}|key={}|voucherId={}",
                    topic, message.getUuid(), message.getKey(), message.getMessageBody().getVoucherId());
        }
    }

    /**
     * 截断过长字符串，避免 header/日志膨胀。
     */
    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= 256 ? s : s.substring(0, 256);
    }

    /**
     * 安静休眠：捕获中断并仅设置中断标记。
     */
    private void sleepQuietly(long backoffMs) {
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 防御式指标自增：meterRegistry 为空或异常时吞掉错误。
     */
    private void safeInc(String name, String tagKey, String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter(name, tagKey, tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }
}