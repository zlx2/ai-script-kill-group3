package com.wn.interceptors;


import com.wn.entity.user.Userinfo;
import com.wn.utils.JWTUtil;
import com.wn.utils.content.UserContent;
import com.wn.utils.enmu.TokenEnum;
import com.wn.utils.exception.GlobalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 拦截器：登录状态拦截器，对refreshToken进行认证
 */
@Component
@Slf4j //用于自动注入日志对象，省去手动写 Logger 的代码。
public class AuthInterceptor implements HandlerInterceptor {
    @Autowired
    RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("======= AuthInterceptor 拦截到请求：{} =======", request.getRequestURI());
        //1.获取前端request传入的 请求头的某个值 refreshToken
        String refreshToken = request.getHeader("refreshToken");
        log.info("获取到的 refreshToken：{}", refreshToken);
        //2.通过redis获取JWT
        if (refreshToken == null){
            //抛出未认证异常
            throw GlobalException.unauthorized();
        }
        //刷新 Redis 中的 token
        String jwt = (String) redisTemplate.opsForHash().get(refreshToken, "token");
        log.info("从 Redis 查到的 jwt：{}", jwt);
        if (jwt == null){
            throw GlobalException.unauthorized();
        }
        //3.JWT 没过期，允许放行（同时，在redis中缓存用户的名字和id）
        TokenEnum verify = JWTUtil.verify(jwt);
        /**
         * 判断token是否过期
         * TOKEN_EXPIRE：过期
         * TOKEN_BAD:被篡改（后端把JWT传输给前端，被修改）
         * TOKEN_SUCCESS：未过期
         */
        if (verify.equals(TokenEnum.TOKEN_SUCCESS)){
            //后端的ThreadLocal来暂存 用户id和用户名
            Long userId = JWTUtil.getUserId(jwt);
            String username = JWTUtil.getUname(jwt);
            //内存的缓存
            extracted(userId, username);
            return true;
        }else if(verify.equals(TokenEnum.TOKEN_EXPIRE)){
            //1. jwt过期，但是refreshToken没过期，要续期！
            Userinfo userInfo = (Userinfo) redisTemplate.opsForHash().get(refreshToken, "userinfoPO");
            //2.jwt 需要重新生成！
            String new_jwt = JWTUtil.generateToken(userInfo.getUsername(), userInfo.getId());
            redisTemplate.opsForHash().putAll(refreshToken, Map.of("token",new_jwt,"userinfoPO",userInfo));
            redisTemplate.expire(refreshToken,60, TimeUnit.MINUTES);
            extracted(userInfo.getId(), userInfo.getUsername());
            //true 放行，false，拦截！
            return true;
        }else{
            //bad 已经不存在了 jwt 不会发给前端，所以不会被篡改
            return false;
        }
    }

    /**
     *  内存缓存,将用户名和id存入线程中
     */
    private void extracted(Long userId, String username) {
        UserContent.setUserIdHolder(userId);
        UserContent.setUsernameHolder(username);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
