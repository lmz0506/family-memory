package com.familymemory.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JoinFamilyRequest {
    @NotBlank(message = "请输入邀请码")
    private String inviteCode;
    private String displayName;
    private String role;
}
