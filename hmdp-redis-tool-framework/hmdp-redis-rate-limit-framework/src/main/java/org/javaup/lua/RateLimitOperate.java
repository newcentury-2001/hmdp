package org.javaup.lua;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.javaup.redis.RedisCache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Slf4j
public class RateLimitOperate {
    
    private final RedisCache redisCache;
    
    public RateLimitOperate(RedisCache redisCache) {
        this.redisCache = redisCache;
    }
    
    private DefaultRedisScript<Integer> redisScript;
    
    @PostConstruct
    public void init(){
        try {
            redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rateLimit.lua")));
            redisScript.setResultType(Integer.class);
        } catch (Exception e) {
            log.error("redisScript init lua error",e);
        }
    }
    
    public Integer execute(List<String> keys, String[] args){
        return (Integer)redisCache.getInstance().execute(redisScript, keys, args);
    }
}
