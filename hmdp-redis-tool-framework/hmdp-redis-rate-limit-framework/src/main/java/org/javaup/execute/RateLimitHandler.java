package org.javaup.execute;
import org.javaup.ratelimit.extension.RateLimitScene;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 限流处理器接口
 * @author: 阿星不是程序员
 **/
public interface RateLimitHandler {
   
    void execute(Long voucherId, Long userId, RateLimitScene scene);
}
