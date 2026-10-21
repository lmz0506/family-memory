package com.familymemory.dto;

import lombok.Data;
import java.util.List;

/** 按年份后台预加载的轻量索引批次，不包含正文和媒体。 */
@Data
public class UniverseBatchResponse {
    /** 归档年份，对应 fm_universe_index.node_date 的年份。 */
    private int year;
    /** 当前批次条目，仅含索引ID、来源、归档日期和标题。 */
    private List<UniverseItemResponse> items;
    /** 下批查询的排他游标，来源表：fm_universe_index.id；空批次沿用请求游标。 */
    private long nextAfterId;
    /** 是否仍有下一批，true有，false该年已读取完毕。 */
    private boolean hasMore;
}
