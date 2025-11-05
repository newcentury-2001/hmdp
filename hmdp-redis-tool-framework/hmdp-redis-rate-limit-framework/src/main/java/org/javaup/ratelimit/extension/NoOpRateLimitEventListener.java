package org.javaup.ratelimit.extension;

import lombok.extern.slf4j.Slf4j;
import org.javaup.enums.BaseCode;

import java.util.concurrent.atomic.LongAdder;

/**
 * 默认事件监听实现：提供基础日志与计数，不改变主流程行为。
 */
@Slf4j
public class NoOpRateLimitEventListener implements RateLimitEventListener {

    private static final LongAdder beforeExecuteCounter = new LongAdder();
    private static final LongAdder allowedCounter = new LongAdder();
    private static final LongAdder blockedCounter = new LongAdder();

    @Override
    public void onBeforeExecute(RateLimitContext ctx) {
        beforeExecuteCounter.increment();
        if (log.isDebugEnabled()) {
            log.debug("rate-limit.before: voucherId={}, userId={}, ip={}, useSliding={}, keys={}",
                    ctx.getVoucherId(), ctx.getUserId(), ctx.getClientIp(), ctx.isUseSliding(), ctx.getKeys());
        }
    }

    @Override
    public void onAllowed(RateLimitContext ctx) {
        allowedCounter.increment();
        if (log.isDebugEnabled()) {
            log.debug("rate-limit.allowed: voucherId={}, userId={}, ip={}, result={}",
                    ctx.getVoucherId(), ctx.getUserId(), ctx.getClientIp(), ctx.getResult());
        }
    }

    @Override
    public void onBlocked(RateLimitContext ctx, BaseCode reason) {
        blockedCounter.increment();
        log.warn("rate-limit.blocked: reason={}, voucherId={}, userId={}, ip={}, window(ip={},user={}), attempts(ip={},user={})",
                reason,
                ctx.getVoucherId(), ctx.getUserId(), ctx.getClientIp(),
                ctx.getIpWindowMillis(), ctx.getUserWindowMillis(),
                ctx.getIpMaxAttempts(), ctx.getUserMaxAttempts());
    }

    public long getBeforeExecuteCount(){
        return beforeExecuteCounter.sum();
    }
    public long getAllowedCount(){
        return allowedCounter.sum();
    }
    public long getBlockedCount(){
        return blockedCounter.sum();
    }
}