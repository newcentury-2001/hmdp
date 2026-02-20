package org.javaup.config;

import org.javaup.handler.BloomFilterHandlerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 布隆过滤器自动配置类
 * @author: 阿星不是程序员
 **/
@ImportAutoConfiguration(RedissonCommonAutoConfiguration.class)
@EnableConfigurationProperties(BloomFilterProperties.class)
@AutoConfigureAfter(RedissonCommonAutoConfiguration.class)
public class BloomFilterAutoConfiguration {
    
    @Bean
    public BloomFilterHandlerFactory bloomFilterHandlerFactory(){
        return new BloomFilterHandlerFactory();
    }

    @Bean
    public BloomFilterHandlerRegistrar bloomFilterHandlerRegistrar(Environment environment){
        return new BloomFilterHandlerRegistrar(environment);
    }
}
