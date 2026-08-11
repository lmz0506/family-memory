package com.familymemory.dto;

import lombok.Data;

@Data
public class InviteRequest {
    private Integer expireHours = 24;
    private Integer maxUses = 5;
}
