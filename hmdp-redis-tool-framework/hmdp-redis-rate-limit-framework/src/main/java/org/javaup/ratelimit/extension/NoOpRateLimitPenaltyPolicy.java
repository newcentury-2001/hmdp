package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 空操作限流惩罚策略
 * @author: 阿星不是程序员
 **/
public class NoOpRateLimitPenaltyPolicy implements RateLimitPenaltyPolicy {
    @Override
    public void apply(RateLimitContext ctx, BaseCode reason) {
        // no-op
    }
}