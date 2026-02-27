package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆点赞实体
 */
@Data
@TableName("fm_memory_like")
public class MemoryLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long memoryId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
