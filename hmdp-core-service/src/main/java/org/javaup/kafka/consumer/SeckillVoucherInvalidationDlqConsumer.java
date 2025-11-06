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

    // 生产者：用于将重放后的失效广播重新投递到原始 Topic
    @Resource
    private SeckillVoucherInvalidationProducer invalidationProducer;

    // Redis：用于短期去重，避免重复重放导致的消息风暴
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // Micrometer 指标：记录 DLQ 重放的尝试、跳过、成功等行为
    @Resource
    private MeterRegistry meterRegistry;

    // 重放去重窗口（秒）：默认 60 秒，限制同一券的短期重复重放
    @Value("${seckill.cache.invalidate.dlq.replay.dedupWindowSeconds:60}")
    private long replayDedupWindowSeconds;

    // 审计日志：结构化记录重放行为，支持问题追踪与合规审计
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    /**
     * 构造函数：声明消费的消息类型，供父类进行统一反序列化与消费流程处理。
     */
    public SeckillVoucherInvalidationDlqConsumer() {
        super(SeckillVoucherInvalidationMessage.class);
    }

    /**
     * Kafka 监听 DLQ：接收字符串消息并委派父类进行统一解析与消费。
     *
     * 保持最小职责：只转交给父类处理，避免在监听层做业务逻辑。
     */
    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC + ".DLQ"},
            groupId = "${prefix.distinction.name:hmdp}-seckill_voucher_cache_invalidation_dlq-${random.uuid}"
    )
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        consumeRaw(value, key, headers);
    }

    /**
     * DLQ 重放消费逻辑：
     * 1) 校验载荷有效性（voucherId 不为空），否则跳过并打点；
     * 2) 构建 60s 去重键，使用 setIfAbsent + TTL 限制短期内只重放一次；
     * 3) 构造重放 headers，标记来源于 DLQ（dlqReplayCount、dlqReason）；
     * 4) 审计日志与指标上报，便于监控和排障；
     * 5) 重新投递到原始 Topic，实现补偿重放。
     */
    @Override
    protected void doConsume(MessageExtend<SeckillVoucherInvalidationMessage> message) {
        SeckillVoucherInvalidationMessage body = message.getMessageBody();
        // 校验载荷有效性：voucherId 不能为空
        if (Objects.isNull(body.getVoucherId())) {
            log.warn("DLQ消息载荷为空或voucherId缺失, uuid={}", message.getUuid());
            safeInc("seckill_invalidation_dlq_replay_skipped", "reason", "invalid_payload");
            return;
        }

        Long voucherId = body.getVoucherId();
        String reason = body.getReason();
        String dlqReason = message.getHeaders() == null ? null : message.getHeaders().get("dlqReason");
        String originalTopic = SpringUtil.getPrefixDistinctionName() + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;

        // 60s 内对同一个 voucherId 只允许重放一次，防止异常情况下反复重放导致风暴
        String replayKey = "seckill_voucher_invalidation:dlq_replay:" + voucherId;
        Boolean firstReplay = stringRedisTemplate.opsForValue().setIfAbsent(replayKey, "1", replayDedupWindowSeconds, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(firstReplay)) {
            log.warn("跳过重复DLQ重放，voucherId={}, uuid={}, dlqReason={} ", voucherId, message.getUuid(), dlqReason);
            safeInc("seckill_invalidation_dlq_replay_skipped", "reason", "duplicate_within_window");
            return;
        }

        // 构造重放 headers，标记来源于 DLQ，并带上原因与计数
        Map<String, String> headers = message.getHeaders() == null ? new HashMap<>(8) : new HashMap<>(message.getHeaders());
        headers.put("dlqReplayCount", "1");
        if (dlqReason != null) {
            headers.put("dlqReason", dlqReason);
        }

        // 记录审计日志与指标，并将补偿消息重新投递到原始 Topic
        log.warn("执行DLQ重放：voucherId={}, uuid={}, reason={}, dlqReason={}", voucherId, message.getUuid(), reason, dlqReason);
        auditLog.warn("DLQ_REPLAY_ATTEMPT|topic={}|uuid={}|key={}|voucherId={}|dlqReason={}",
                originalTopic, message.getUuid(), message.getKey(), voucherId, dlqReason);
        safeInc("seckill_invalidation_dlq_replay_attempts", "topic", originalTopic);
        invalidationProducer.sendPayload(originalTopic, message.getKey(),
                new SeckillVoucherInvalidationMessage(voucherId, reason), headers);
        safeInc("seckill_invalidation_dlq_replay_sent", "topic", originalTopic);
    }

    /**
     * 安全计数：隔离指标上报中的异常，避免影响主流程。
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