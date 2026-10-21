package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 家庭空间实体，对应表：fm_family。
 */
@Data
@TableName("fm_family")
public class Family {
    /**
     * 主键ID，对应表：fm_family.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 家庭名称。
     */
    private String name;
    /**
     * 家庭介绍。
     */
    private String description;
    /**
     * 封面图片地址。
     */
    private String coverUrl;
    /**
     * 创建者ID，来源表：fm_user.id。
     */
    private Long creatorId;
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
