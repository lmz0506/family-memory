package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭大事记提醒实体，对应表：fm_family_milestone。
 */
@Data
@TableName("fm_family_milestone")
public class FamilyMilestone {
    /**
     * 主键ID，对应表：fm_family_milestone.id。
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
     * 标题，最多120字。
     */
    private String title;
    /**
     * 提醒说明。
     */
    private String description;
    /**
     * 提醒对应的原始日期。
     */
    private LocalDate eventDate;
    /**
     * 每年重复标志，0一次性提醒，1每年重复。
     */
    private Integer recurringYearly;
    /**
     * 可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员。
     */
    private String visibilityScope;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 删除标志，0有效，1已删除。
     */
    @TableLogic
    private Integer deleted;
}
