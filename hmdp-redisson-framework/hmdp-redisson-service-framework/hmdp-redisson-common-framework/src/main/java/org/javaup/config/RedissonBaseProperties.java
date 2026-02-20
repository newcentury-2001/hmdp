package org.javaup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.concurrent.TimeUnit;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: Redisson基础配置属性
 * @author: 阿星不是程序员
 **/
@Data
@ConfigurationProperties(prefix = "spring.redis.redisson")
public class RedissonBaseProperties {

    private Integer threads = 16;
    
    private Integer nettyThreads = 32;
    
    private Integer corePoolSize = null;
   
    private Integer maximumPoolSize = null;
    
    private long keepAliveTime = 30;
    
    private TimeUnit unit = TimeUnit.SECONDS;
  
    private Integer workQueueSize = 256; 
}
