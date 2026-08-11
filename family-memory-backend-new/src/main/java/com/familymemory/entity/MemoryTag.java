package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fm_memory_tag")
public class MemoryTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memoryId;
    private String tagName;
    private LocalDateTime createdAt;
}
