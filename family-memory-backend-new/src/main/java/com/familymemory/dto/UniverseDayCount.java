package com.familymemory.dto;

import lombok.Data;
import java.time.LocalDate;

/** 数据库按天聚合结果，不包含内容ID、标题或正文。 */
@Data
public class UniverseDayCount {
    /** 归档日期，来源表：fm_universe_index.node_date。 */
    private LocalDate nodeDate;
    /** 当天记忆数量，来源类型 MEMORY。 */
    private long memoryCount;
    /** 当天珍藏数量，来源类型 CAPSULE。 */
    private long capsuleCount;
}
