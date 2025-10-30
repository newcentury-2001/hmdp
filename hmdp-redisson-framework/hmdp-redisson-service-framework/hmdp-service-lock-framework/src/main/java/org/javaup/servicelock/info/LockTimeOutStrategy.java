package org.javaup.servicelock.info;


/**
 * @program: 数据中台实战项目。 添加 阿星不是程序员 微信，添加时备注 中台 来获取项目的完整资料 
 * @description: 分布式锁 策略
 * @author: 阿星不是程序员
 **/
public enum LockTimeOutStrategy implements LockTimeOutHandler{
    /**
     * 快速失败
     * */
    FAIL(){
        @Override
        public void handler(String lockName) {
            String msg = String.format("%s请求频繁",lockName);
            throw new RuntimeException(msg);
        }
    }
}
