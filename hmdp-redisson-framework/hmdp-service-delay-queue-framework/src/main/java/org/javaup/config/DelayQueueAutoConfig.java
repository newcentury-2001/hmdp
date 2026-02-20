package org.javaup.config;


import org.javaup.context.DelayQueueBasePart;
import org.javaup.context.DelayQueueContext;
import org.javaup.event.DelayQueueInitHandler;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列自动配置类
 * @author: 阿星不是程序员
 **/
@Slf4j
@Configuration
@EnableConfigurationProperties(DelayQueueProperties.class)
public class DelayQueueAutoConfig {
    
    public DelayQueueAutoConfig() {
        log.info("[DelayQueueAutoConfig] 初始化延迟队列配置类");
    }
    
    @Bean
    public DelayQueueInitHandler delayQueueInitHandler(DelayQueueBasePart delayQueueBasePart){
        log.info("[DelayQueueAutoConfig] 创建 DelayQueueInitHandler Bean");
        return new DelayQueueInitHandler(delayQueueBasePart);
    }
   
    @Bean
    public DelayQueueBasePart delayQueueBasePart(RedissonClient redissonClient,DelayQueueProperties delayQueueProperties){
        log.info("[DelayQueueAutoConfig] 创建 DelayQueueBasePart Bean");
        return new DelayQueueBasePart(redissonClient,delayQueueProperties);
    }
  
    @Bean
    public DelayQueueContext delayQueueContext(DelayQueueBasePart delayQueueBasePart){
        log.info("[DelayQueueAutoConfig] 创建 DelayQueueContext Bean");
        return new DelayQueueContext(delayQueueBasePart);
    }
}
