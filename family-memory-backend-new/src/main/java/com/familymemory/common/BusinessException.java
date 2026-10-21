package com.familymemory.common;

import lombok.Getter;

/**
 * 带业务状态码的异常，由全局异常处理器转换为统一响应。
 */
@Getter
public class BusinessException extends RuntimeException {
    /**
     * 业务状态码，200成功，400参数错误，401未登录，403无权限，404不存在，500服务异常。
     */
    private final int code;

    /**
     * 构造包含业务状态码和提示消息的异常。
     * @param code 业务状态码，与统一响应状态码一致
     * @param message 业务提示消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造400参数错误异常。
     * @param message 业务提示消息
     * @return 包含状态码和提示消息的业务异常
     */
    public static BusinessException badRequest(String message) {
        return new BusinessException(400, message);
    }

    /**
     * 构造401未登录异常。
     * @param message 业务提示消息
     * @return 包含状态码和提示消息的业务异常
     */
    public static BusinessException unauthorized(String message) {
        return new BusinessException(401, message);
    }

    /**
     * 构造403无权限异常。
     * @param message 业务提示消息
     * @return 包含状态码和提示消息的业务异常
     */
    public static BusinessException forbidden(String message) {
        return new BusinessException(403, message);
    }

    /**
     * 构造404记录不存在异常。
     * @param message 业务提示消息
     * @return 包含状态码和提示消息的业务异常
     */
    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }
}
