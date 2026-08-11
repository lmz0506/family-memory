package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 30, message = "用户名长度为 3-30 位")
    private String username;
    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 30, message = "密码长度为 6-30 位")
    private String password;
    @NotBlank(message = "请输入昵称")
    @Size(max = 30, message = "昵称最多 30 个字")
    private String nickname;
    private String phone;
}
