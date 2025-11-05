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
import org.javaup.ratelimit.extension.RateLimitContext;
import org.javaup.ratelimit.extension.RateLimitEventListener;
import org.javaup.ratelimit.extension.RateLimitPenaltyPolicy;
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
    private final RateLimitEventListener rateLimitEventListener;
    private final RateLimitPenaltyPolicy rateLimitPenaltyPolicy;

    public RedisRateLimitHandler(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                 RedisCache redisCache,
                                 RateLimitOperate rateLimitOperate,
                                 SlidingRateLimitOperate slidingRateLimitOperate,
                                 RateLimitEventListener rateLimitEventListener,
                                 RateLimitPenaltyPolicy rateLimitPenaltyPolicy) {
        this.seckillRateLimitConfigProperties = seckillRateLimitConfigProperties;
        this.redisCache = redisCache;
        this.rateLimitOperate = rateLimitOperate;
        this.slidingRateLimitOperate = slidingRateLimitOperate;
        this.rateLimitEventListener = rateLimitEventListener;
        this.rateLimitPenaltyPolicy = rateLimitPenaltyPolicy;
    }

    @Override
    public void execute(Long voucherId, 
                        Long userId) {
        String clientIp = resolveClientIp();

        if (isWhitelisted(userId, clientIp)) {
            return;
        }

        checkBans(voucherId, userId, clientIp);

        int ipLimitWindowMillis = seckillRateLimitConfigProperties.getIpWindowMillis();
        int ipLimitMaxAttempts = seckillRateLimitConfigProperties.getIpMaxAttempts();
        int userLimitWindowMillis = seckillRateLimitConfigProperties.getUserWindowMillis();
        int userLimitMaxAttempts = seckillRateLimitConfigProperties.getUserMaxAttempts();

        boolean useSliding = Boolean.TRUE.equals(seckillRateLimitConfigProperties.getEnableSlidingWindow());
        List<String> keys = buildRateLimitKeys(voucherId, userId, clientIp, useSliding);
        String[] args = buildArgs(ipLimitWindowMillis, ipLimitMaxAttempts, userLimitWindowMillis, userLimitMaxAttempts);

        RateLimitContext ctx = buildContext(voucherId, userId, clientIp, keys, useSliding,
                ipLimitWindowMillis, ipLimitMaxAttempts, userLimitWindowMillis, userLimitMaxAttempts);
        safeBeforeExecute(ctx);

        Integer result = executeLua(useSliding, keys, args);
        ctx.setResult(result);
        handleResult(ctx);
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

    private boolean isWhitelisted(Long userId, 
                                  String clientIp) {
        try {
            return (clientIp != null && seckillRateLimitConfigProperties.getIpWhitelist() != null
                    && seckillRateLimitConfigProperties.getIpWhitelist().contains(clientIp))
                    || (userId != null && seckillRateLimitConfigProperties.getUserWhitelist() != null
                    && seckillRateLimitConfigProperties.getUserWhitelist().contains(userId));
        } catch (Exception e) {
            return false;
        }
    }

    private void checkBans(Long voucherId, 
                           Long userId, 
                           String clientIp) {
        
        if (Objects.nonNull(clientIp)) {
            boolean ipBlocked = Boolean.TRUE.equals(redisCache.hasKey(
                    RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_BLOCK_IP_TAG_KEY, voucherId, clientIp)));
            if (ipBlocked) {
                throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED);
            }
        }
        boolean userBlocked = Boolean.TRUE.equals(redisCache.hasKey(
                RedisKeyBuild.createRedisKey(RedisKeyManage.SECKILL_BLOCK_USER_TAG_KEY, voucherId, userId)));
        if (userBlocked) {
            throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED);
        }
    }

    private List<String> buildRateLimitKeys(Long voucherId, 
                                            Long userId, 
                                            String clientIp, 
                                            boolean useSliding) {
        List<String> keys = new ArrayList<>(2);
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
        return keys;
    }

    private String[] buildArgs(int ipWindowMillis, 
                               int ipMaxAttempts, 
                               int userWindowMillis, 
                               int userMaxAttempts) {
        String[] args = new String[4];
        args[0] = String.valueOf(ipWindowMillis);
        args[1] = String.valueOf(ipMaxAttempts);
        args[2] = String.valueOf(userWindowMillis);
        args[3] = String.valueOf(userMaxAttempts);
        return args;
    }

    private RateLimitContext buildContext(Long voucherId, 
                                          Long userId, 
                                          String clientIp, 
                                          List<String> keys,
                                          boolean useSliding,
                                          int ipWindowMillis, int ipMaxAttempts,
                                          int userWindowMillis, int userMaxAttempts) {
        return new RateLimitContext(
                voucherId,
                userId,
                clientIp,
                keys,
                useSliding,
                ipWindowMillis,
                ipMaxAttempts,
                userWindowMillis,
                userMaxAttempts
        );
    }

    private void safeBeforeExecute(RateLimitContext ctx) {
        rateLimitEventListener.onBeforeExecute(ctx);
    }

    private Integer executeLua(boolean useSliding, List<String> keys, String[] args) {
        return useSliding ? slidingRateLimitOperate.execute(keys, args) : rateLimitOperate.execute(keys, args);
    }

    private void handleResult(RateLimitContext ctx) {
        Integer result = ctx.getResult();
        if (BaseCode.SUCCESS.getCode().equals(result)) {
            rateLimitEventListener.onAllowed(ctx);
            return;
        }
        if (BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED.getCode().equals(result)) {
            rateLimitEventListener.onBlocked(ctx, BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED);
            rateLimitPenaltyPolicy.apply(ctx, BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED);
            throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_IP_EXCEEDED);
        }
        if (BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED.getCode().equals(result)) {
            rateLimitEventListener.onBlocked(ctx, BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED);
            rateLimitPenaltyPolicy.apply(ctx, BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED);
            throw new HmdpFrameException(BaseCode.SECKILL_RATE_LIMIT_USER_EXCEEDED);
        }
        throw new HmdpFrameException("操作频繁，请稍后再试");
    }
}
