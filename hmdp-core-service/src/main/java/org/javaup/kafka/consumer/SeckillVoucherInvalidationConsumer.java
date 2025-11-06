package org.javaup.kafka.consumer;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.javaup.cache.SeckillVoucherLocalCache;
import org.javaup.consumer.AbstractConsumerHandler;
import org.javaup.core.RedisKeyManage;
import org.javaup.kafka.message.SeckillVoucherInvalidationMessage;
import org.javaup.message.MessageExtend;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

import static org.javaup.constant.Constant.SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC;
import static org.javaup.constant.Constant.SPRING_INJECT_PREFIX_DISTINCTION_NAME;


/**
 * Kafka 消费者：接收“秒杀券缓存失效”广播并执行本地/Redis缓存清理。
 * 负责：
 * 1) 失效本地缓存，缩短不一致窗口；
 * 2) 幂等删除 Redis 的券详情、库存、空值键；
 * 3) 记录结构化日志，异常场景打印警告。
 */
@Slf4j
@Component
public class SeckillVoucherInvalidationConsumer extends AbstractConsumerHandler<SeckillVoucherInvalidationMessage> {

    // 本地缓存：用于当前实例的快速读取
    @Resource
    private SeckillVoucherLocalCache seckillVoucherLocalCache;

    // Redis 缓存：用于跨实例共享的券详情与库存
    @Resource
    private RedisCache redisCache;

    public SeckillVoucherInvalidationConsumer() {
        super(SeckillVoucherInvalidationMessage.class);
    }

    /**
     * Kafka 消息入口：转交统一消费流程。
     */
    @KafkaListener(
            topics = {SPRING_INJECT_PREFIX_DISTINCTION_NAME + "-" + SECKILL_VOUCHER_CACHE_INVALIDATION_TOPIC},
            groupId = "${prefix.distinction.name:hmdp}-seckill_voucher_cache_invalidation-${random.uuid}"
    )
    public void onMessage(String value,
                          @Headers Map<String, Object> headers,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        consumeRaw(value, key, headers);
    }

    @Override
    /**
     * 核心消费：校验载荷 -> 本地缓存失效 -> Redis 幂等删除 -> 记录日志。
     */
    protected void doConsume(MessageExtend<SeckillVoucherInvalidationMessage> message) {
        SeckillVoucherInvalidationMessage body = message.getMessageBody();
        if (Objects.isNull(body.getVoucherId())) {
            log.warn("收到缓存失效消息但载荷为空或voucherId缺失, uuid={}", message.getUuid());
            return;
        }
        Long voucherId = body.getVoucherId();

        // 1) 失效本地缓存
        seckillVoucherLocalCache.invalidate(voucherId);

        // 2) 删除Redis缓存（券详情、库存、空值）——幂等删除
        try {
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_TAG_KEY, voucherId));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_STOCK_TAG_KEY, voucherId));
            redisCache.del(RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_VOUCHER_NULL_TAG_KEY, voucherId));
        } catch (Exception e) {
            log.warn("删除Redis缓存失败 voucherId={}", voucherId, e);
        }

        log.info("完成缓存失效：voucherId={}, reason={}", voucherId, body.getReason());
    }
}