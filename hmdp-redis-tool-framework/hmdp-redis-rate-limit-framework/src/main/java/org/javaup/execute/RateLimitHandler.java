package org.javaup.execute;

public interface RateLimitHandler {

    /**
     * 执行限流与Lua脚本的综合处理
     * @param voucherId 秒杀券ID
     */
    void execute(Long voucherId,Long userId);
}
