package com.wn.entity;

import org.springframework.http.HttpStatus;

import java.util.HashMap;

/**
 * 统一响应码对象
 **/
public class R extends HashMap<String, Object> {
    //枚举类，用于定义ResponseEntity的key
    public enum ResponseStatus {
        //code 响应编码
        CODE("code"),
        //message 响应消息
        MSG("msg"),
        //data响应体
        DATA("data");
        private final String value;

        ResponseStatus(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }

    private static final String CODE = ResponseStatus.CODE.value();//当做map中的key
    private static final String MSG = ResponseStatus.MSG.value();
    private static final String DATA = ResponseStatus.DATA.value(); // 修复：统一命名为DATA

    public R() {

    }

    // 新增：单消息构造函数，适配全局异常处理器使用
    public R(String message) {
        super.put(CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        super.put(MSG, message);
    }

    public R(Object obj) {
        super.put(CODE, HttpStatus.OK.value());
        super.put(MSG, "执行成功");
        super.put(DATA, obj);
    }

    public R(int code, String message) {
        super.put(CODE, code);
        super.put(MSG, message);
    }

    public R(int code, String message, Object data) {
        super.put(CODE, code);
        super.put(MSG, message);
        if (data != null) {
            super.put(DATA, data);
        }
    }

    // 新增：静态成功方法（规范写法，替代常量，避免共享实例污染）
    public static R success() {
        return new R(HttpStatus.OK.value(), "执行成功");
    }

    // 新增：静态成功带数据方法
    public static R success(Object data) {
        return new R(data);
    }

    // 新增：静态错误方法，异常处理器专用
    public static R error(int code, String message) {
        return new R(code, message);
    }

    // 新增：静态错误方法，默认500错误码
    public static R error(String message) {
        return new R(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    // 保留原有常量，兼容你旧代码写法（不推荐继续使用，建议改用success()/error()方法）
    public static final R SUCCESS = new R(HttpStatus.OK.value(), "执行成功！");
    public static final R ERROR = new R(HttpStatus.INTERNAL_SERVER_ERROR.value(), "执行失败！");
    public static final R TIME_OUT = new R(HttpStatus.REQUEST_TIMEOUT.value(), "降级成功");

    public R putKey(String key, Object obj) {
        super.put(key, obj);
        return this;
    }
}