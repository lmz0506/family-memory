package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆点赞实体，对应表：fm_memory_like。
 */
@Data
@TableName("fm_memory_like")
public class MemoryLike {
    /**
     * 主键ID，对应表：fm_memory_like.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 记忆ID，来源表：fm_memory.id。
     */
    private Long memoryId;
    /**
     * 用户ID，来源表：fm_user.id。
     */
    private Long userId;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
