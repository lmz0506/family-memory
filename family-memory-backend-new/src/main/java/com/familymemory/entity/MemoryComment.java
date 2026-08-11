package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fm_memory_comment")
public class MemoryComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memoryId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer deleted;
}
