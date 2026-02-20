package org.javaup.lockinfo;

import org.aspectj.lang.JoinPoint;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 锁信息处理接口
 * @author: 阿星不是程序员
 **/
public interface LockInfoHandle {
   
    String getLockName(JoinPoint joinPoint, String name, String[] keys);
    
    String simpleGetLockName(String name,String[] keys);
}
