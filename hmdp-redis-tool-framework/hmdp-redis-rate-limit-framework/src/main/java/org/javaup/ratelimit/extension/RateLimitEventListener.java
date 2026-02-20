package org.javaup.ratelimit.extension;

import org.javaup.enums.BaseCode;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 限流事件监听器
 * @author: 阿星不是程序员
 **/
public interface RateLimitEventListener {

    /**
     * 脚本执行前回调（已计算出keys与参数）
     */
    void onBeforeExecute(RateLimitContext ctx);

    /**
     * 允许通过时回调
     */
    void onAllowed(RateLimitContext ctx);

    /**
     * 命中限流阻断时回调（区分 IP / 用户）
     */
    void onBlocked(RateLimitContext ctx, BaseCode reason);
}