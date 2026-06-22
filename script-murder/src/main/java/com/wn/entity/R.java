package com.wn.entity;

import org.springframework.http.HttpStatus;

import java.util.HashMap;

/**
 * 统一响应码对象
 **/
public class R extends HashMap {
    //枚举类，用于定义ResponseEntity的key
    public enum ResponseStatus{
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
        public String value(){
            return this.value;
        }
    }
    private static final String CODE= ResponseStatus.CODE.value();//当做map中的key
    private static final String MSG= ResponseStatus.MSG.value;
    private static final String OBJ = ResponseStatus.DATA.value();
    public R(){

    }
    public R(Object obj) {
        super.put(CODE, HttpStatus.OK.value());
        super.put(MSG,"执行成功");
        super.put(OBJ,obj);
    }
    public R(int code, String message ){
        super.put(CODE,code);
        super.put(MSG,message);
    }
    public R(int code, String message, Object data) {
        super.put(CODE,code);
        super.put(MSG,message);
        if (data != null) {
            super.put(OBJ,data);
        }
    }

    public static final R SUCCESS=new R(HttpStatus.OK.value(),"执行成功！");
    public static final R ERROR=new R(HttpStatus.INTERNAL_SERVER_ERROR.value(),"执行失败！");
    public static final R TIME_OUT=new R(HttpStatus.REQUEST_TIMEOUT.value(),"降级成功");

    public R putKey(String key,Object obj){
        super.put(key,obj);
        return  this;
    }
}