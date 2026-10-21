package com.familymemory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 星海轻量索引。正文、媒体及珍藏开启状态仍由原业务保存。 */
@Data
@TableName("fm_universe_index")
public class UniverseIndex {
    /** 索引主键，对应 fm_universe_index.id。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 家庭ID，来源表：fm_family.id。 */
    private Long familyId;
    /** 来源类型，MEMORY 记忆，CAPSULE 珍藏。 */
    private String sourceType;
    /** 来源ID，MEMORY 来源表：fm_memory.id；CAPSULE 来源表：fm_time_capsule.id。 */
    private Long sourceId;
    /** 归档日期，记忆取 fm_memory.memory_date，珍藏取 fm_time_capsule.created_at 的日期部分；发布后固定。 */
    private LocalDate nodeDate;
    /** 来源标题，来源表：fm_memory.title / fm_time_capsule.title，业务修改时同步。 */
    private String title;
    /** 索引行创建时间。 */
    private LocalDateTime createdAt;
    /** 索引行最近更新时间。 */
    private LocalDateTime updatedAt;
    /** 删除标志，0 有效，1 已删除。 */
    @TableLogic
    private Integer deleted;
}
