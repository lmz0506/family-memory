package com.familymemory.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应，包含业务状态码、消息和响应数据。
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    /**
     * 业务状态码，200成功，400参数错误，401未登录，403无权限，404不存在，500服务异常。
     */
    private int code;
    /**
     * 响应提示消息。
     */
    private String message;
    /**
     * 响应数据，失败或无返回内容时为空。
     */
    private T data;

    /**
     * 构造成功响应。
     * @param <T> 响应数据类型
     * @param data 响应数据，无内容时可为null
     * @return 统一接口响应及业务结果
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "success", data);
    }

    /**
     * 构造成功响应。
     * @param <T> 响应数据类型
     * @param message 业务提示消息
     * @param data 响应数据，无内容时可为null
     * @return 统一接口响应及业务结果
     */
    public static <T> ApiResult<T> ok(String message, T data) {
        return new ApiResult<>(200, message, data);
    }

    /**
     * 构造失败响应。
     * @param code 业务状态码，与统一响应状态码一致
     * @param message 业务提示消息
     * @return 统一接口响应及业务结果
     */
    public static ApiResult<Void> fail(int code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
