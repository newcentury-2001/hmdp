package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

/**
 * 默认空实现：不做任何处理，确保可插拔。
 */
public class NoOpRateLimitEventListener implements RateLimitEventListener {
    @Override
    public void onBeforeExecute(RateLimitContext ctx) {
        // no-op
    }

    @Override
    public void onAllowed(RateLimitContext ctx) {
        // no-op
    }

    @Override
    public void onBlocked(RateLimitContext ctx, BaseCode reason) {
        // no-op
    }
}