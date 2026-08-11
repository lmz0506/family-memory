package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fm_memory")
public class Memory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long familyId;
    private Long authorId;
    private String type;
    private String title;
    private String content;
    private LocalDate memoryDate;
    private Integer memoryYear;
    private String location;
    private String audioUrl;
    private Integer audioDuration;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
