package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("fm_memory_media")
public class MemoryMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memoryId;
    private String mediaType;
    private String url;
    private String thumbnailUrl;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
