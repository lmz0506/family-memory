package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 登录响应
 */
@Data
public class LoginResp {

    private String token;
    private UserResp user;
}
