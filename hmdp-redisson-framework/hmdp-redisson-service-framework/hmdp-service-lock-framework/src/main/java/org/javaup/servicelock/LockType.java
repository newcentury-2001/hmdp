package org.javaup.servicelock;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 锁类型枚举
 * @author: 阿星不是程序员
 **/
public enum LockType {
    /**
     * 锁类型
     */
    Reentrant,
    
    Fair,
   
    Read,
    
    Write;

    LockType() {
    }

}
