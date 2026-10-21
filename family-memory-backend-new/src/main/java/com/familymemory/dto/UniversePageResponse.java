package com.familymemory.dto;

import lombok.Data;
import java.util.List;

/** 星海日期列表及标题搜索的分页响应。 */
@Data
public class UniversePageResponse {
    /** 当前查询匹配的总条数。 */
    private long total;
    /** 当前页，从1开始。 */
    private int page;
    /** 实际分页大小，范围1至50。 */
    private int size;
    /** 当前页条目，仅含索引定位字段及标题。 */
    private List<UniverseItemResponse> items;
    /** 是否还有下一页。 */
    private boolean hasMore;
}
