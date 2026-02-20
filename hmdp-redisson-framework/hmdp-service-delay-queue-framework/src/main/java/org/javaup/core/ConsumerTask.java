package org.javaup.core;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 消费者任务
 * @author: 阿星不是程序员
 **/
public interface ConsumerTask {
    
    void execute(String content);
  
    String topic();
}
