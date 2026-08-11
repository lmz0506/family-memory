package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fm_family_milestone")
public class FamilyMilestone {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyId;
    private Long creatorId;
    private String title;
    private String description;
    private LocalDate eventDate;
    private Integer recurringYearly;
    private String visibilityScope;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
