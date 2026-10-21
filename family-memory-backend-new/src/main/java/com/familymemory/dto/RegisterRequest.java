package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 新用户注册请求。
 */
@Data
public class RegisterRequest {
    /**
     * 登录用户名。
     */
    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 30, message = "用户名长度为 3-30 位")
    private String username;
    /**
     * 登录密码明文，仅用于请求校验，不得记录日志或返回。
     */
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 30, message = "密码长度为 6-30 位")
    private String password;
    /**
     * 用户显示昵称。
     */
    @NotBlank(message = "请输入昵称")
    @Size(max = 30, message = "昵称最多 30 个字")
    private String nickname;
    /**
     * 手机号码。
     */
    private String phone;
}
