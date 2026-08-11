package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fm_time_capsule")
public class TimeCapsule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyId;
    private Long creatorId;
    private String recipient;
    private String title;
    private String content;
    private String coverUrl;
    private LocalDate openDate;
    private Integer isOpened;
    private LocalDateTime openedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
