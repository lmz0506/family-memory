package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时光胶囊实体
 */
@Data
@TableName("fm_time_capsule")
public class TimeCapsule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long creatorId;

    private String title;

    private String content;

    private LocalDate openDate;

    private Integer isOpened;

    private LocalDateTime openedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
