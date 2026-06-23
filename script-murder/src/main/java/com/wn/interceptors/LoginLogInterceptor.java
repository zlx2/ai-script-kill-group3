package com.wn.interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 专门用于记录登录日志，拦截登录请求并记录日志
 */
@Component
@Slf4j
public class LoginLogInterceptor implements HandlerInterceptor {
    //前置拦截
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("Request {} {} from {} ",  // {} 是占位符
                request.getMethod(),        // 请求方法，例如：GET / POST
                request.getRequestURI(),    // 请求的uri，例如：/auth / /user/list
                request.getRemoteAddr());   // 请求地址，例如：192.168.1.100
        return true;
    }
}
