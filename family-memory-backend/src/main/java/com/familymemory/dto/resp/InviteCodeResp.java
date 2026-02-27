package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 邀请码响应
 */
@Data
public class InviteCodeResp {

    private String code;
    private String expireAt;
    private String inviteLink;
}
