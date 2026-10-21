package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆标签实体，对应表：fm_memory_tag。
 */
@Data
@TableName("fm_memory_tag")
public class MemoryTag {
    /**
     * 主键ID，对应表：fm_memory_tag.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 记忆ID，来源表：fm_memory.id。
     */
    private Long memoryId;
    /**
     * 标签名称，同一记忆下唯一。
     */
    private String tagName;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
