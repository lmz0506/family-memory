package com.familymemory.dto.resp;

import lombok.Data;

import java.util.List;

/**
 * 记忆响应
 */
@Data
public class MemoryResp {

    private Long id;
    private String type;
    private String title;
    private String description;
    private String date;
    private String year;
    private String memoryTime;
    private String author;
    private Long authorId;
    private String avatar;
    private String location;
    private List<String> images;
    private String audioUrl;
    private Integer audioDuration;
    private Integer likes;
    private List<String> tags;
    private Boolean isLiked;
    private String createdAt;
    private Integer yearsAgo;
}
