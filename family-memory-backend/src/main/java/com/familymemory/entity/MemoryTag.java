package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆标签实体
 */
@Data
@TableName("fm_memory_tag")
public class MemoryTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memoryId;

    private String tagName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
