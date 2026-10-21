package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭邀请码实体，对应表：fm_invite_code。
 */
@Data
@TableName("fm_invite_code")
public class InviteCode {
    /**
     * 主键ID，对应表：fm_invite_code.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 家庭ID，来源表：fm_family.id。
     */
    private Long familyId;
    /**
     * 创建者ID，来源表：fm_user.id。
     */
    private Long creatorId;
    /**
     * 家庭邀请码，8位且唯一。
     */
    private String code;
    /**
     * 邀请码到期时间。
     */
    private LocalDateTime expireAt;
    /**
     * 允许使用的最大次数。
     */
    private Integer maxUses;
    /**
     * 已使用次数。
     */
    private Integer usedCount;
    /**
     * 邀请码状态，1可用，0停用或次数已用尽。
     */
    private Integer status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
