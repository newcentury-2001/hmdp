package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

/**
 * 默认空实现：不做任何惩罚处理。
 */
public class NoOpRateLimitPenaltyPolicy implements RateLimitPenaltyPolicy {
    @Override
    public void apply(RateLimitContext ctx, BaseCode reason) {
        // no-op
    }
}