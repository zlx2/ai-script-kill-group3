package com.wn.utils.exception;

import com.wn.entity.R;
import com.wn.service.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常 BusinessException
     */
    @ExceptionHandler(BusinessException.class)
    public R handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: path={}, message={}", request.getRequestURI(), e.getMessage());
        // 业务异常统一返回500错误码，消息为自定义提示
        return R.error(500, e.getMessage());
    }

    /**
     * 捕获参数校验类异常（参数缺失、类型不匹配、校验失败）
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public R handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("请求参数异常: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return R.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    /**
     * 捕获文件上传异常
     */
    @ExceptionHandler(MultipartException.class)
    public R handleMultipartException(MultipartException e, HttpServletRequest request) {
        log.error("📁 文件上传解析失败: path={}, message={}", request.getRequestURI(), e.getMessage(), e);
        return R.error(HttpStatus.BAD_REQUEST.value(), "文件上传失败，请检查文件格式！错误：" + e.getMessage());
    }

    /**
     * 兜底捕获所有未知系统异常
     * 注意：这个方法必须放在最下面，前面异常都匹配不到才会走这里
     */
    @ExceptionHandler(Exception.class)
    public R handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        return R.error("系统繁忙，请稍后再试");
    }
}