package org.javaup.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.javaup.dto.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.javaup.utils.RedisConstants.LOGIN_USER_KEY;
import static org.javaup.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 刷新token拦截器
 * @author: 阿星不是程序员
 **/
@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        log.info("RefreshTokenInterceptor - 请求URI: {}, Method: {}", uri, request.getMethod());
        
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");
        log.info("RefreshTokenInterceptor - Token: {}", token);
        
        if (StrUtil.isBlank(token)) {
            log.info("RefreshTokenInterceptor - Token为空，直接放行");
            return true;
        }
        // 2.基于TOKEN获取redis中的用户
        String key  = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3.判断用户是否存在
        if (userMap.isEmpty()) {
            log.info("RefreshTokenInterceptor - Redis中未找到用户，直接放行");
            return true;
        }
        // 5.将查询到的hash数据转为UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6.存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        log.info("RefreshTokenInterceptor - 从Redis加载用户到ThreadLocal: {}", userDTO.getNickName());
        // 7.刷新token有效期（按秒设置，避免 Redisson pExpire 递归问题）
        stringRedisTemplate.expire(
                key,
                TimeUnit.SECONDS.convert(LOGIN_USER_TTL, TimeUnit.MINUTES),
                TimeUnit.SECONDS
        );
        // 8.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
        log.info("RefreshTokenInterceptor - 清理ThreadLocal");
    }
}
