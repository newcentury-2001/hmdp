package org.javaup.redis.config;

import org.javaup.redis.RedisCache;
import org.javaup.redis.RedisCacheImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: Redis缓存自动配置类
 * @author: 阿星不是程序员
 **/
@Configuration
public class RedisCacheAutoConfig {
    
    @Bean
    public RedisCache redisCache(StringRedisTemplate stringRedisTemplate){
        return new RedisCacheImpl(stringRedisTemplate);
    }
}
