package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 记忆实体
 */
@Data
@TableName("fm_memory")
public class Memory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long authorId;

    private String type;

    private String title;

    private String description;

    private LocalDate memoryDate;

    private String memoryYear;

    private LocalDateTime memoryTime;

    private String location;

    private String audioUrl;

    private Integer audioDuration;

    private Integer likesCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
