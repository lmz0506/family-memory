package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭实体
 */
@Data
@TableName("fm_family")
public class Family {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String avatar;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
