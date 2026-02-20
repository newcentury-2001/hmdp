package org.javaup.ratelimit.extension;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 限流场景
 * @author: 阿星不是程序员
 **/
public enum RateLimitScene {
    /** 发令牌接口 */
    ISSUE_TOKEN,
    /** 下单（秒杀）接口 */
    SECKILL_ORDER
}