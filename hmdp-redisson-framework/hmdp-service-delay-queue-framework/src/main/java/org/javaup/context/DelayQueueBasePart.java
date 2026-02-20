package org.javaup.context;

import org.javaup.config.DelayQueueProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.redisson.api.RedissonClient;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列基础部分
 * @author: 阿星不是程序员
 **/
@Data
@AllArgsConstructor
public class DelayQueueBasePart {
    
    private final RedissonClient redissonClient;
    
    private final DelayQueueProperties delayQueueProperties;
}
