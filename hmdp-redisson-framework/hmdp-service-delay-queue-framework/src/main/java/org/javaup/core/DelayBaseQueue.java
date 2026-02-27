package org.javaup.core;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列基类
 * @author: 阿星不是程序员
 **/
@Slf4j
public class DelayBaseQueue {
    
    protected final RedissonClient redissonClient;
    protected final RBlockingQueue<String> blockingQueue;
    protected final String relTopic;
    
    
    public DelayBaseQueue(RedissonClient redissonClient, String relTopic){
        this.redissonClient = redissonClient;
        this.relTopic = relTopic;
        log.info("[DelayBaseQueue] 初始化阻塞队列: {}", relTopic);
        this.blockingQueue = redissonClient.getBlockingQueue(relTopic);
        log.info("[DelayBaseQueue] 阻塞队列初始化完成: {}", relTopic);
    }
}
