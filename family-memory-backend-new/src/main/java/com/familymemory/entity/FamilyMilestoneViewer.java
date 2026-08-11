package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fm_family_milestone_viewer")
public class FamilyMilestoneViewer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long milestoneId;
    private Long userId;
    private LocalDateTime createdAt;
}
