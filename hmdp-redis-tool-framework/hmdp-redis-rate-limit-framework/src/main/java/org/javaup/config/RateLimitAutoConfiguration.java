package org.javaup.config;

import org.javaup.execute.RedisRateLimitHandler;
import org.javaup.lua.RateLimitOperate;
import org.javaup.redis.RedisCache;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @program: 极度真实还原大麦网高并发实战项目。 添加 阿星不是程序员 微信，添加时备注 大麦 来获取项目的完整资料 
 * @description: 布隆过滤器 配置
 * @author: 阿星不是程序员
 **/
@EnableConfigurationProperties(SeckillRateLimitConfigProperties.class)
public class RateLimitAutoConfiguration {
    
    @Bean
    public RateLimitOperate rateLimitOperate(RedisCache redisCache){
        return new RateLimitOperate(redisCache);
    }

    @Bean
    public RedisRateLimitHandler redisRateLimitHandler(SeckillRateLimitConfigProperties seckillRateLimitConfigProperties,
                                                       RedisCache redisCache,
                                                       RateLimitOperate rateLimitOperate) {
        return new RedisRateLimitHandler(
                seckillRateLimitConfigProperties, 
                redisCache, 
                rateLimitOperate
        );
    }
}
