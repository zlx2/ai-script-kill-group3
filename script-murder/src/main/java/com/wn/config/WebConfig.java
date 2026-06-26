package com.wn.config;


import com.wn.interceptors.AuthInterceptor;
import com.wn.interceptors.LoginLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 拦截器
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final LoginLogInterceptor loginLogInterceptor;
    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //做日志记录，拦截所有登录 order:数字越小越先执行
        registry.addInterceptor(loginLogInterceptor).addPathPatterns("/**").order(1);
        //认证
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .order(2)
                .excludePathPatterns(List.of(
                        "/auth/login",
                        "/auth/register",
                        "/auth/code",
                        "/game/**",
                        "/ai/expand"
                ));
    }
}
