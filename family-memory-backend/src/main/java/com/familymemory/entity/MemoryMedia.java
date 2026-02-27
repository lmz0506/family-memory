package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆媒体实体
 */
@Data
@TableName("fm_memory_media")
public class MemoryMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memoryId;

    private String mediaType;

    private String url;

    private String thumbnailUrl;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
