package org.javaup.servicelock.info;


/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 锁超时策略
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
