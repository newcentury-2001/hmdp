package org.javaup.lockinfo.impl;

import org.javaup.lockinfo.AbstractLockInfoHandle;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 服务锁信息处理
 * @author: 阿星不是程序员
 **/
public class ServiceLockInfoHandle extends AbstractLockInfoHandle {

    private static final String LOCK_PREFIX_NAME = "SERVICE_LOCK";
    
    @Override
    protected String getLockPrefixName() {
        return LOCK_PREFIX_NAME;
    }
}
