package org.javaup.execute;

import jakarta.servlet.http.HttpServletRequest;
import org.javaup.config.SeckillRateLimitConfigProperties;
import org.javaup.core.RedisKeyManage;
import org.javaup.enums.BaseCode;
import org.javaup.exception.HmdpFrameException;
import org.javaup.lua.RateLimitOperate;
import org.javaup.lua.SlidingRateLimitOperate;
import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisKeyBuild;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 统一封装：按IP与用户限流 + 调用Lua脚本扣减与记录
 */
public class RedisRateLimitHandler implements RateLimitHandler {

    private final SeckillRateLimitConfigProperties seckillRateLimitConfigProperties;
    private final RedisCache redisCache;
    private final RateLimitOperate rateLimitOperate;
    private final SlidingRateLimitOperate slidingRateLimitOperate;

    public RedisRateLimitHandler(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                 RedisCache redisCache,
                                 RateLimitOperate rateLimitOperate,
                                 SlidingRateLimitOperate slidingRateLimitOperate) {
        this.seckillRateLimitConfigProperties = seckillRateLimitConfigProperties;
        this.redisCache = redisCache;
        this.rateLimitOperate = rateLimitOperate;
        this.slidingRateLimitOperate = slidingRateLimitOperate;
    }

    @Override
    public void execute(Long voucherId, Long userId) {

        String clientIp = resolveClientIp();

        // 使用 Lua 执行按 IP 与用户的限流（毫秒单位）
        final int ipLimitWindowMillis = seckillRateLimitConfigProperties.getIpWindowMillis();
        final int ipLimitMaxAttempts = seckillRateLimitConfigProperties.getIpMaxAttempts();
        final int userLimitWindowMillis = seckillRateLimitConfigProperties.getUserWindowMillis();
        final int userLimitMaxAttempts = seckillRateLimitConfigProperties.getUserMaxAttempts();

        // 构造 keys：根据是否启用滑动窗口决定 key 类型
        List<String> keys = new ArrayList<>(2);
        boolean useSliding = Boolean.TRUE.equals(seckillRateLimitConfigProperties.getEnableSlidingWindow());
        if (Objects.nonNull(clientIp)) {
            String ipKey = useSliding
                    ? RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_LIMIT_IP_SW_TAG_KEY, voucherId, clientIp).getRelKey()
                    : RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_LIMIT_IP_TAG_KEY, voucherId, clientIp).getRelKey();
            keys.add(ipKey);
        }
        String userKey = useSliding
                ? RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_LIMIT_USER_SW_TAG_KEY, voucherId, userId).getRelKey()
                : RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_LIMIT_USER_TAG_KEY, voucherId, userId).getRelKey();
        keys.add(userKey);

        // 传递窗口与次数配置（毫秒）
        String[] args = new String[4];
        args[0] = String.valueOf(ipLimitWindowMillis);
        args[1] = String.valueOf(ipLimitMaxAttempts);
        args[2] = String.valueOf(userLimitWindowMillis);
        args[3] = String.valueOf(userLimitMaxAttempts);

        Integer result = useSliding
                ? slidingRateLimitOperate.execute(keys, args)
                : rateLimitOperate.execute(keys, args);
        if (BaseCode.SUCCESS.getCode().equals(result)) {
            return;
        }
        if (BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED.getCode().equals(result)) {
            throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED);
        }
        if (BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED.getCode().equals(result)) {
            throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED);
        }
        throw new HmdpFrameException("操作频繁，请稍后再试");
    }
    
    private String resolveClientIp(){
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                String[] parts = xff.split(",");
                if (parts.length > 0) {
                    String ip = parts[0].trim();
                    if (!ip.isEmpty()) {
                        return ip;
                    }
                }
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return realIp;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
