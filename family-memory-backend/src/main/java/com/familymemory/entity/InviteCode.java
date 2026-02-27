package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请码实体
 */
@Data
@TableName("fm_invite_code")
public class InviteCode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyId;

    private Long creatorId;

    private String code;

    private LocalDateTime expireAt;

    private Integer maxUses;

    private Integer usedCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
