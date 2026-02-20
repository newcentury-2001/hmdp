package org.javaup.lockinfo.impl;

import org.javaup.lockinfo.AbstractLockInfoHandle;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 重复执行限制锁信息处理
 * @author: 阿星不是程序员
 **/
public class RepeatExecuteLimitLockInfoHandle extends AbstractLockInfoHandle {

    public static final String PREFIX_NAME = "REPEAT_EXECUTE_LIMIT";
    
    @Override
    protected String getLockPrefixName() {
        return PREFIX_NAME;
    }
}
