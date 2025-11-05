package org.javaup.kafka.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.SpringUtil;
import org.javaup.kafka.message.SeckillVoucherInvalidationMessage;
import org.javaup.kafka.producer.SeckillVoucherInvalidationProducer;
import org.javaup.message.MessageExtend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;

/**
 * 秒杀券缓存失效广播的 DLQ 消费者：用于失败后的补偿重放。
 *
 * 策略：
 * - 仅进行一次快速重放（通过 Redis 做 60s 去重），避免异常情况下重复重放导致风暴；
 * - 重放仍失败将继续进入 DLQ，供后续人工处理或监控告警。
 */
@Slf4j
@Component
public class SeckillVoucherInvalidationDlqConsumer extends AbstractConsumerHandler<SeckillVoucherInvalidationMessage> {

    @Resource
    private SeckillVoucherInvalidationProducer invalidationProducer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MeterRegistry meterRegistry;

    @Value("${seckill.cache.invalidate.dlq.replay.dedupWindowSeconds:60}")
    private long replayDedupWindowSeconds;

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    public SeckillVoucherInvalidationDlqConsumer() {
        super(SeckillVoucherInvalidationMessage.class);
    }

    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC + ".DLQ"},
            groupId = "${prefix.distinction.name:hmdp}-seckill_voucher_cache_invalidation_dlq-${random.uuid}"
    )
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        consumeRaw(value, key, headers);
    }

    @Override
    protected void doConsume(MessageExtend<SeckillVoucherInvalidationMessage> message) {
        SeckillVoucherInvalidationMessage body = message.getMessageBody();
        if (Objects.isNull(body.getVoucherId())) {
            log.warn("DLQ消息载荷为空或voucherId缺失, uuid={}", message.getUuid());
            safeInc("seckill_invalidation_dlq_replay_skipped", "reason", "invalid_payload");
            return;
        }

        Long voucherId = body.getVoucherId();
        String reason = body.getReason();
        String dlqReason = message.getHeaders() == null ? null : message.getHeaders().get("dlqReason");
        String originalTopic = SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;

        // 60s 内对同一个 voucherId 只重放一次，避免异常情况下反复重放
        String replayKey = "seckill_voucher_invalidation:dlq_replay:" + voucherId;
        Boolean firstReplay = stringRedisTemplate.opsForValue().setIfAbsent(replayKey, "1", replayDedupWindowSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(firstReplay)) {
            log.warn("跳过重复DLQ重放，voucherId={}, uuid={}, dlqReason={} ", voucherId, message.getUuid(), dlqReason);
            safeInc("seckill_invalidation_dlq_replay_skipped", "reason", "duplicate_within_window");
            return;
        }

        // 构造重放 headers，标记来源于DLQ
        Map<String, String> headers = message.getHeaders() == null ? new HashMap<>(8) : new HashMap<>(message.getHeaders());
        headers.put("dlqReplayCount", "1");
        if (dlqReason != null) {
            headers.put("dlqReason", dlqReason);
        }

        log.warn("执行DLQ重放：voucherId={}, uuid={}, reason={}, dlqReason={}", voucherId, message.getUuid(), reason, dlqReason);
        auditLog.warn("DLQ_REPLAY_ATTEMPT|topic={}|uuid={}|key={}|voucherId={}|dlqReason={}",
                originalTopic, message.getUuid(), message.getKey(), voucherId, dlqReason);
        safeInc("seckill_invalidation_dlq_replay_attempts", "topic", originalTopic);
        invalidationProducer.sendPayload(originalTopic, message.getKey(),
                new SeckillVoucherInvalidationMessage(voucherId, reason), headers);
        safeInc("seckill_invalidation_dlq_replay_sent", "topic", originalTopic);
    }

    private void safeInc(String name, String tagKey, String tagValue) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter(name, tagKey, tagValue).increment();
            }
        } catch (Exception ignore) {
        }
    }
}