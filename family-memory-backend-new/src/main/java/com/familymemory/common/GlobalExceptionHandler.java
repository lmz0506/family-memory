package com.familymemory.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * 统一处理业务异常、参数校验异常、上传大小异常和未预期异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 转换业务异常，保留其业务状态码和消息。
     * @param e 待转换为统一响应的异常
     * @return 统一接口响应及业务结果
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusiness(BusinessException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 合并字段校验错误，返回400响应。
     * @param e 待转换为统一响应的异常
     * @return 统一接口响应及业务结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleValidation(MethodArgumentNotValidException e) {
        String message =
                e.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("；"));
        return ApiResult.fail(400, message);
    }

    /**
     * 处理上传大小超限，返回400响应。
     * @return 统一接口响应及业务结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleUploadSize() {
        return ApiResult.fail(400, "上传文件超过大小限制");
    }

    /**
     * 隐藏内部异常细节，返回500通用错误。
     * @param e 待转换为统一响应的异常
     * @return 统一接口响应及业务结果
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleUnknown(Exception e) {
        return ApiResult.fail(500, "服务暂时不可用，请稍后重试");
    }
}
