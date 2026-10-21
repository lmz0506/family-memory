package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 珍藏时光胶囊实体，对应表：fm_time_capsule。
 */
@Data
@TableName("fm_time_capsule")
public class TimeCapsule {
    /**
     * 主键ID，对应表：fm_time_capsule.id。
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 家庭ID，来源表：fm_family.id。
     */
    private Long familyId;
    /**
     * 创建者ID，来源表：fm_user.id。
     */
    private Long creatorId;
    /**
     * 收信人称呼。
     */
    private String recipient;
    /**
     * 标题，最多120字。
     */
    private String title;
    /**
     * 封存正文，仅达到开启条件后由详情接口返回。
     */
    private String content;
    /**
     * 封面图片地址。
     */
    private String coverUrl;
    /**
     * 约定开启日期，仅控制内容解锁，不作为星海归档日期。
     */
    private LocalDate openDate;
    /**
     * 开启标志，0尚未记录开启，1已开启；接口还会结合open_date判断是否可读。
     */
    private Integer isOpened;
    /**
     * 首次实际开启时间，未开启时为空。
     */
    private LocalDateTime openedAt;
    /**
     * 封存时间，日期部分作为星海归档日期，发布后固定。
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
