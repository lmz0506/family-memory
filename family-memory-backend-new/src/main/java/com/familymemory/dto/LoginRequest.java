package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 账号密码登录请求。
 */
@Data
public class LoginRequest {
    /**
     * 登录用户名。
     */
    @NotBlank(message = "请输入用户名")
    private String username;
    /**
     * 登录密码明文，仅用于请求校验，不得记录日志或返回。
     */
    @NotBlank(message = "请输入密码")
    private String password;
}
