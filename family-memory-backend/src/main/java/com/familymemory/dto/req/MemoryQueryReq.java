package com.familymemory.dto.req;

import lombok.Data;

/**
 * 记忆查询请求
 */
@Data
public class MemoryQueryReq {

    private Integer page = 1;

    private Integer size = 10;

    /** 年份筛选 */
    private String year;

    /** 类型筛选：photo/video/note */
    private String type;

    /** 关键字搜索（标题、内容） */
    private String keyword;

    /** 发布人ID */
    private Long authorId;

    /** 地点关键字 */
    private String location;

    /** 标签 */
    private String tag;
}
