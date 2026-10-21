package com.familymemory.dto;

import lombok.Data;

/**
 * 家庭邀请码生成参数。
 */
@Data
public class InviteRequest {
    /**
     * 邀请码有效小时数，至少1小时。
     */
    private Integer expireHours = 24;
    /**
     * 邀请码允许使用次数，至少1次。
     */
    private Integer maxUses = 5;
}
