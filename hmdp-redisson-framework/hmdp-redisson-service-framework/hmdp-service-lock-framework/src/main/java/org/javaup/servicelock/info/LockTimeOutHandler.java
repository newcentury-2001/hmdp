package org.javaup.servicelock.info;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 锁超时处理器
 * @author: 阿星不是程序员
 **/
public interface LockTimeOutHandler {
    
    /**
     * 处理
     * @param lockName 锁名
     * */
    void handler(String lockName);
}
