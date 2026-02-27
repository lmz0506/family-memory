package com.familymemory.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 加入家庭请求
 */
@Data
public class JoinFamilyReq {

    @NotBlank(message = "邀请码不能为空")
    private String inviteCode;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String role;
}
