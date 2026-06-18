package com.wn.utils.exception;
import com.wn.entity.R;
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
    //异常的捕获注解
    @ExceptionHandler(GlobalException.class)
    public R handleGlobalException(GlobalException ex, HttpServletRequest request) {
        HttpStatus status = resolveHttpStatus(ex.getCode());
        if (status.is5xxServerError()) {
            log.error("业务异常: path={}, code={}, message={}", request.getRequestURI(), ex.getCode(), ex.getMessage(), ex);
        } else {
            log.warn("业务异常: path={}, code={}, message={}", request.getRequestURI(), ex.getCode(), ex.getMessage());
        }
        return new R(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class
    })
    public R handleBadRequest(Exception ex, HttpServletRequest request) {
        log.warn("请求参数异常: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return new R(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }
    @ExceptionHandler(MultipartException.class)
    public R handleMultipartException(MultipartException e) {
        log.error("📁 文件上传解析失败: {}", e.getMessage(), e);
        return new R(HttpStatus.BAD_REQUEST.value(), "文件上传失败，请检查文件格式！错误：" + e.getMessage());
    }
    //这个异常需要放在最下面！前面的异常 都麽能捕获的时候，用这个！
    @ExceptionHandler(Exception.class)
    public R handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常: path={}, message={}", request.getRequestURI(), ex.getMessage(), ex);
        return new R("系统繁忙，请稍后再试");
    }

    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            default -> code >= 500 ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        };
    }
}
