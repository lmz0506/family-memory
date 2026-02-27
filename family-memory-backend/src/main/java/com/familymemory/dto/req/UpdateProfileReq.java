package com.familymemory.dto.req;

import lombok.Data;

/**
 * 更新个人资料请求
 */
@Data
public class UpdateProfileReq {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 手机号
     */
    private String phone;
}
