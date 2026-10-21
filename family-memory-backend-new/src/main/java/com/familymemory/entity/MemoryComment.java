package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆评论实体，对应表：fm_memory_comment。
 */
@Data
@TableName("fm_memory_comment")
public class MemoryComment {
    /**
     * 主键ID，对应表：fm_memory_comment.id。
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
     * 评论正文，最多500字。
     */
    private String content;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 删除标志，0有效，1已删除。
     */
    @TableLogic
    private Integer deleted;
}
