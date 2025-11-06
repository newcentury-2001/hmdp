package org.javaup.core;


import lombok.Getter;

/**
 * @program: 数据中台实战项目。 添加 阿星不是程序员 微信，添加时备注 中台 来获取项目的完整资料 
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
