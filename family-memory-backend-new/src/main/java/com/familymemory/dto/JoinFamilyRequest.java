package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 使用邀请码加入家庭的请求。
 */
@Data
public class JoinFamilyRequest {
    /**
     * 家庭邀请码，来源表：fm_invite_code.code。
     */
    @NotBlank(message = "请输入邀请码")
    private String inviteCode;
    /**
     * 家庭内称呼。
     */
    private String displayName;
    /**
     * 家庭角色，自由文本，例如家庭成员。
     */
    private String role;
}
