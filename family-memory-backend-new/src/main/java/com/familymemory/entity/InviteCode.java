package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
}
