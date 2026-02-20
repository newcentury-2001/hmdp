package org.javaup.config;

import org.javaup.constant.LockInfoType;
import org.javaup.core.ManageLocker;
import org.javaup.lockinfo.LockInfoHandle;
import org.javaup.lockinfo.factory.LockInfoHandleFactory;
import org.javaup.lockinfo.impl.ServiceLockInfoHandle;
import org.javaup.servicelock.aspect.ServiceLockAspect;
import org.javaup.servicelock.factory.ServiceLockFactory;
import org.javaup.util.ServiceLockTool;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 服务锁自动配置类
 * @author: 阿星不是程序员
 **/
@Configuration
public class ServiceLockAutoConfiguration {
    
    @Bean(LockInfoType.SERVICE_LOCK)
    public LockInfoHandle serviceLockInfoHandle(){
        return new ServiceLockInfoHandle();
    }
    
    @Bean
    public ManageLocker manageLocker(RedissonClient redissonClient){
        return new ManageLocker(redissonClient);
    }
    
    @Bean
    public ServiceLockFactory serviceLockFactory(ManageLocker manageLocker){
        return new ServiceLockFactory(manageLocker);
    }
    
    @Bean
    public ServiceLockAspect serviceLockAspect(LockInfoHandleFactory lockInfoHandleFactory,ServiceLockFactory serviceLockFactory){
        return new ServiceLockAspect(lockInfoHandleFactory,serviceLockFactory);
    }
    
    @Bean
    public ServiceLockTool serviceLockTool(LockInfoHandleFactory lockInfoHandleFactory,ServiceLockFactory serviceLockFactory){
        return new ServiceLockTool(lockInfoHandleFactory,serviceLockFactory);
    }
}
