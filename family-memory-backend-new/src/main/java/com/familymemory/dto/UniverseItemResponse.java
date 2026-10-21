package com.familymemory.dto;

import lombok.Data;
import java.time.LocalDate;

/** 日期列表和标题搜索返回的轻量条目；查看内容需调用原业务详情接口。 */
@Data
public class UniverseItemResponse {
    /** 星海索引ID，来源表：fm_universe_index.id，可用于前端稳定标识节点。 */
    private Long indexId;
    /** 来源类型，MEMORY 记忆，CAPSULE 珍藏。 */
    private String sourceType;
    /** 来源ID，MEMORY 来源表：fm_memory.id；CAPSULE 来源表：fm_time_capsule.id。 */
    private Long sourceId;
    /** 归档日期，来源表：fm_universe_index.node_date。 */
    private LocalDate nodeDate;
    /** 标题，来源表：fm_universe_index.title。不包含正文和珍藏开启内容。 */
    private String title;
}
