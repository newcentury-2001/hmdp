package org.javaup.context;

import org.javaup.core.ConsumerTask;
import lombok.Data;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列部分
 * @author: 阿星不是程序员
 **/
@Data
public class DelayQueuePart {
    
    private final DelayQueueBasePart delayQueueBasePart;
 
    private final ConsumerTask consumerTask;
    
    public DelayQueuePart(DelayQueueBasePart delayQueueBasePart, ConsumerTask consumerTask){
        this.delayQueueBasePart = delayQueueBasePart;
        this.consumerTask = consumerTask;
    }
}
