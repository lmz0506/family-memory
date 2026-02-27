package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 家庭统计响应
 */
@Data
public class FamilyStatisticsResp {

    private Long recordDays;
    private Long photoCount;
    private Long memoryCount;
    private Long memberCount;
    private Long capsuleCount;
}
