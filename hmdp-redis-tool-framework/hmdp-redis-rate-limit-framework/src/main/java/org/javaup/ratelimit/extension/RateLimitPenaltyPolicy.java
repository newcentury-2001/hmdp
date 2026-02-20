package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 限流惩罚策略
 * @author: 阿星不是程序员
 **/
public interface RateLimitPenaltyPolicy {

    /**
     * 应用惩罚策略
     * @param ctx    当前限流上下文
     * @param reason 命中原因（IP/USER 限流等）
     */
    void apply(RateLimitContext ctx, BaseCode reason);
}