package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记忆图片与视频实体，对应表：fm_memory_media。
 */
@Data
@TableName("fm_memory_media")
public class MemoryMedia {
    /**
     * 主键ID，对应表：fm_memory_media.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 记忆ID，来源表：fm_memory.id。
     */
    private Long memoryId;
    /**
     * 媒体类型，image图片，video视频。
     */
    private String mediaType;
    /**
     * 原始媒体资源地址。
     */
    private String url;
    /**
     * 媒体缩略图地址，未生成时为空。
     */
    private String thumbnailUrl;
    /**
     * 媒体展示顺序，按数值升序。
     */
    private Integer sortOrder;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
