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
    
    SECKILL_STOCK_KEY("seckill:stock:%s","秒杀券id","value为库存","k"),
    
    SECKILL_VOUCHER_KEY("seckill:voucher:%s","秒杀券id","value为SeckillVoucher类型","k"),
    
    SECKILL_VOUCHER_KEY_NULL("seckill:voucher_null:%s","秒杀券id","value为这是空值","k"),
    
    SECKILL_USER_KEY("seckill:user:%s","秒杀券id","value为用户的集合","k"),
    
    DB_SECKILL_ORDER_KEY("db:seckill:order:%s","秒杀券的订单id","value为订单","k"),
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
