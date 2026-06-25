/**
 * @Author: 鱼
 * @Description:
 * @DateTime: 2026/6/23 11:16
 * @Component:
 **/
package com.wn.service.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}