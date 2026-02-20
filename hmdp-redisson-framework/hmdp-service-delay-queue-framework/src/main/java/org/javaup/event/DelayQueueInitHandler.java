package org.javaup.event;

import cn.hutool.core.collection.CollectionUtil;
import org.javaup.context.DelayQueueBasePart;
import org.javaup.context.DelayQueuePart;
import org.javaup.core.ConsumerTask;
import org.javaup.core.DelayConsumerQueue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 延迟队列初始化处理器
 * @author: 阿星不是程序员
 **/
@Slf4j
@AllArgsConstructor
public class DelayQueueInitHandler implements ApplicationListener<ApplicationStartedEvent> {
    
    private final DelayQueueBasePart delayQueueBasePart;
    
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.info("[DelayQueue] 开始初始化延迟队列框架...");

        Map<String, ConsumerTask> consumerTaskMap = event.getApplicationContext().getBeansOfType(ConsumerTask.class);
        if (CollectionUtil.isEmpty(consumerTaskMap)) {
            log.info("[DelayQueue] 未找到任何ConsumerTask实现类，延迟队列框架初始化完成");
            return;
        }
        
        log.info("[DelayQueue] 找到 {} 个ConsumerTask实现类", consumerTaskMap.size());
        
        for (Map.Entry<String, ConsumerTask> entry : consumerTaskMap.entrySet()) {
            String beanName = entry.getKey();
            ConsumerTask consumerTask = entry.getValue();
            String topic = consumerTask.topic();
            
            log.info("[DelayQueue] 初始化消费者: {}，主题: {}", beanName, topic);
            
            DelayQueuePart delayQueuePart = new DelayQueuePart(delayQueueBasePart, consumerTask);
            Integer isolationRegionCount = delayQueuePart.getDelayQueueBasePart().getDelayQueueProperties()
                    .getIsolationRegionCount();
            
            log.info("[DelayQueue] 为消费者 {} 创建 {} 个隔离分区", beanName, isolationRegionCount);
            
            for(int i = 0; i < isolationRegionCount; i++) {
                String queueName = topic + "-" + i;
                DelayConsumerQueue delayConsumerQueue = new DelayConsumerQueue(delayQueuePart, queueName);
                log.info("[DelayQueue] 启动延迟队列监听线程: {}", queueName);
                delayConsumerQueue.listenStart();
            }
        }
        
        log.info("[DelayQueue] 延迟队列框架初始化完成");
    }
}
