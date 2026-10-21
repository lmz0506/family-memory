package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭大事记指定可见成员实体，对应表：fm_family_milestone_viewer。
 */
@Data
@TableName("fm_family_milestone_viewer")
public class FamilyMilestoneViewer {
    /**
     * 主键ID，对应表：fm_family_milestone_viewer.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 大事记ID，来源表：fm_family_milestone.id。
     */
    private Long milestoneId;
    /**
     * 用户ID，来源表：fm_user.id。
     */
    private Long userId;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
