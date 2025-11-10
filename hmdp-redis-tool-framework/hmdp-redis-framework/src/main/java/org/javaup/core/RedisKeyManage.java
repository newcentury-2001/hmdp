package org.javaup.core;


import lombok.Getter;

/**
 * @program: 黑马点评-plus升级版实战项目。添加 阿星不是程序员 微信，添加时备注 点评 来获取项目的完整资料
 * @description: redis key管理
 * @author: 阿星不是程序员
 **/
@Getter
public enum RedisKeyManage {
    /**
     * redis 缓存 key管理
     * */
    CACHE_SHOP_KEY("cache:shop:%s","商铺id","value为Shop类型","k"),
    
    CACHE_SHOP_KEY_NULL("cache:shop_null:%s","商铺id","value为这是空值","k"),

    /**
     * 单槽位库存（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_STOCK_TAG_KEY("seckill:stock:{%s}","秒杀券id（同槽位HashTag）","value为库存","k"),

    /**
     * 单槽位用户集合（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_USER_TAG_KEY("seckill:user:{%s}","秒杀券id（同槽位HashTag）","value为已下单用户集合","k"),

    /**
     * 单槽位券详情（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_VOUCHER_TAG_KEY("seckill:voucher:{%s}","秒杀券id（同槽位HashTag）","value为SeckillVoucher类型","k"),

    /**
     * 单槽位券详情空值（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_VOUCHER_NULL_TAG_KEY("seckill:voucher_null:{%s}","秒杀券id（同槽位HashTag）","value为这是空值","k"),
    
    /**
     * 单槽位操作记录日志（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_TRACE_LOG_TAG_KEY("seckill:trace:log:{%s}","秒杀券id（同槽位HashTag）","value为操作记录日志","k"),
    
    /**
     * 单槽位IP限流计数器（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_LIMIT_IP_TAG_KEY("seckill:limit:ip:{%s}:%s","秒杀券id（同槽位HashTag）","value为按IP的限流计数","k"),

    /**
     * 单槽位用户限流计数器（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_LIMIT_USER_TAG_KEY("seckill:limit:user:{%s}:%s","秒杀券id（同槽位HashTag）","value为按用户的限流计数","k"),

    /**
     * 单槽位IP滑动窗口限流计数器（ZSET，voucherId在同槽位）
     */
    SECKILL_LIMIT_IP_SW_TAG_KEY("seckill:limit:ip:sw:{%s}:%s","秒杀券id（同槽位HashTag）","value为按IP的滑动窗口计数","k"),

    /**
     * 单槽位用户滑动窗口限流计数器（ZSET，voucherId在同槽位）
     */
    SECKILL_LIMIT_USER_SW_TAG_KEY("seckill:limit:user:sw:{%s}:%s","秒杀券id（同槽位HashTag）","value为按用户的滑动窗口计数","k"),

    /**
     * 单槽位IP令牌桶限流（HASH，voucherId在同槽位）
     */
    SECKILL_LIMIT_IP_TB_TAG_KEY("seckill:limit:ip:tb:{%s}:%s","秒杀券id（同槽位HashTag）","value为按IP的令牌桶HASH（tokens/last_ms）","k"),

    /**
     * 单槽位用户令牌桶限流（HASH，voucherId在同槽位）
     */
    SECKILL_LIMIT_USER_TB_TAG_KEY("seckill:limit:user:tb:{%s}:%s","秒杀券id（同槽位HashTag）","value为按用户的令牌桶HASH（tokens/last_ms）","k"),

    /**
     * 单槽位IP封禁标记（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_BLOCK_IP_TAG_KEY("seckill:block:ip:{%s}:%s","秒杀券id（同槽位HashTag）","value为按IP的临时封禁标记","k"),

    /**
     * 单槽位用户封禁标记（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_BLOCK_USER_TAG_KEY("seckill:block:user:{%s}:%s","秒杀券id（同槽位HashTag）","value为按用户的临时封禁标记","k"),

    /**
     * 单槽位IP违规计数器（带Hash Tag，voucherId在同槽位），统计被阻断次数
     */
    SECKILL_VIOLATION_IP_TAG_KEY("seckill:violation:ip:{%s}:%s","秒杀券id（同槽位HashTag）","value为按IP的违规计数","k"),

    /**
     * 单槽位用户违规计数器（带Hash Tag，voucherId在同槽位），统计被阻断次数
     */
    SECKILL_VIOLATION_USER_TAG_KEY("seckill:violation:user:{%s}:%s","秒杀券id（同槽位HashTag）","value为按用户的违规计数","k"),
    
    DB_SECKILL_ORDER_KEY("db:seckill:order:%s","秒杀券的订单id","value为订单","k"),
    
    SECKILL_ROLLBACK_ALERT_DEDUP_KEY("seckill:rollback:alert:dedup:%s","秒杀券的id","value为1","k"),
    
    /**
     * 自动发券成功通知的去重键（带HashTag，voucherId在同槽位）
     * member 维度包含 voucherId 与 userId，避免重复提醒
     */
    SECKILL_AUTO_ISSUE_NOTIFY_DEDUP_KEY("seckill:autoissue:notify:dedup:{%s}:%s","秒杀券id（同槽位HashTag）与用户id","value为1","k"),

    /**
     * 开抢提醒通知的去重键（带HashTag，voucherId在同槽位）
     * member 维度包含 voucherId 与 userId，避免重复提醒
     */
    SECKILL_REMINDER_NOTIFY_DEDUP_KEY("seckill:reminder:notify:dedup:{%s}:%s","秒杀券id（同槽位HashTag）与用户id","value为1","k"),

    /**
     * 秒杀访问令牌键（带HashTag，voucherId在同槽位；第二占位为userId）
     */
    SECKILL_ACCESS_TOKEN_TAG_KEY("seckill:access:token:{%s}:%s","秒杀券id（同槽位HashTag）与用户id","访问令牌","k"),
    
    /**
     * 订阅：用户集合（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_SUBSCRIBE_USER_TAG_KEY("seckill:subscribe:user:{%s}","秒杀券id（同槽位HashTag）","value为订阅用户集合","k"),
    
    /**
     * 订阅：队列ZSET（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_SUBSCRIBE_ZSET_TAG_KEY("seckill:subscribe:zset:{%s}","秒杀券id（同槽位HashTag）","value为订阅队列，member为用户id，score为加入时间戳","k"),
    
    /**
     * 订阅：状态HASH（带Hash Tag，voucherId在同槽位）
     */
    SECKILL_SUBSCRIBE_STATUS_TAG_KEY("seckill:subscribe:status:{%s}","秒杀券id（同槽位HashTag）","value为用户订阅状态HASH，field为用户id，value为状态码","k"),
    
    /**
     * 店铺维度：每日Top买家统计（ZSET，带Hash Tag，shopId同槽位）
     * member 为用户id，score 为当日购买次数
     */
    SECKILL_SHOP_TOP_BUYERS_DAILY_TAG_KEY("seckill:shop:topbuyers:daily:{%s}:%s","商铺id（同槽位HashTag）与日期(yyyyMMdd)","ZSET，member为用户id，score为购买次数","k"),

    /**
     * 店铺维度：Top买家聚合临时键（ZSET，带Hash Tag，shopId同槽位）
     * 将多日的每日ZSET并集聚合到该临时键
     */
    SECKILL_SHOP_TOP_BUYERS_UNION_TAG_KEY("seckill:shop:topbuyers:union:{%s}:%s","商铺id（同槽位HashTag）与聚合范围","临时ZSET，member为用户id，score为购买次数合并","k"),
    
    ;

    /**
     * key值
     * */
    private final String key;

    /**
     * key的说明
     * */
    private final String keyIntroduce;

    /**
     * value的说明
     * */
    private final String valueIntroduce;

    /**
     * 作者
     * */
    private final String author;

    RedisKeyManage(String key, String keyIntroduce, String valueIntroduce, String author){
        this.key = key;
        this.keyIntroduce = keyIntroduce;
        this.valueIntroduce = valueIntroduce;
        this.author = author;
    }

    public static RedisKeyManage getRc(String keyCode) {
        for (RedisKeyManage re : RedisKeyManage.values()) {
            if (re.key.equals(keyCode)) {
                return re;
            }
        }
        return null;
    }
    
}
