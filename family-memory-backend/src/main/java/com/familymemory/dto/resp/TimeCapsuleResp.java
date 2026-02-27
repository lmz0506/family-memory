package com.familymemory.dto.resp;

import lombok.Data;

/**
 * 时光胶囊响应
 */
@Data
public class TimeCapsuleResp {

    private Long id;
    private String title;
    private String content;
    private String openDate;
    private Boolean isOpened;
    private String openedAt;
    private String creatorName;
    private String creatorAvatar;
    private String createdAt;
}
