package com.wn.utils.exception;

import org.springframework.http.HttpStatus;
public class GlobalException extends RuntimeException {
    private final Integer code;
    private final String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public GlobalException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public static GlobalException unauthorized() {
        return new GlobalException(HttpStatus.UNAUTHORIZED.value(),"认证失败，请重新登录！");
    }
    public static GlobalException forbidden() {
        return new GlobalException(HttpStatus.FORBIDDEN.value(),"拒绝访问，请重新登录！");
    }
    public static GlobalException notFound() {
        return new GlobalException(HttpStatus.NOT_FOUND.value(),"路径错误！");
    }
}
