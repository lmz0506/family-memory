package com.familymemory.dto.req;

import lombok.Data;

/**
 * 生成邀请码请求
 */
@Data
public class CreateInviteCodeReq {

    private Integer expireHours = 24;

    private Integer maxUses = 1;
}
