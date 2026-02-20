package org.javaup.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @program: 黑马点评-plus升级版实战项目
 * @description: 登录拦截器
 * @author: 阿星不是程序员
 **/
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        log.info("LoginInterceptor - 请求URI: {}, Method: {}", uri, request.getMethod());
        
        // 1.判断是否需要拦截（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            // 没有，需要拦截，设置状态码
            log.warn("LoginInterceptor - 用户未登录，拦截请求: {}", uri);
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户，则放行
        log.info("LoginInterceptor - 用户已登录，放行请求: {}", uri);
        return true;
    }
}
