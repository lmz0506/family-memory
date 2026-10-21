package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 家庭记忆实体，对应表：fm_memory。
 */
@Data
@TableName("fm_memory")
public class Memory {
    /**
     * 主键ID，对应表：fm_memory.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 家庭ID，来源表：fm_family.id。
     */
    private Long familyId;
    /**
     * 发布者ID，来源表：fm_user.id。
     */
    private Long authorId;
    /**
     * 记忆类型，photo照片，video视频，note文字，audio声音。
     */
    private String type;
    /**
     * 标题，最多120字。
     */
    private String title;
    /**
     * 正文内容。
     */
    private String content;
    /**
     * 事情实际发生日期，发布后固定，也是星海归档日期。
     */
    private LocalDate memoryDate;
    /**
     * 记忆年份，取memory_date的年份，用于传统时间轴筛选。
     */
    private Integer memoryYear;
    /**
     * 记忆发生地点。
     */
    private String location;
    /**
     * 音频资源地址。
     */
    private String audioUrl;
    /**
     * 音频时长，单位秒。
     */
    private Integer audioDuration;
    /**
     * 累计点赞数。
     */
    private Integer likesCount;
    /**
     * 累计评论数。
     */
    private Integer commentsCount;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 最近更新时间。
     */
    private LocalDateTime updatedAt;
    /**
     * 删除标志，0有效，1已删除。
     */
    @TableLogic
    private Integer deleted;
}
