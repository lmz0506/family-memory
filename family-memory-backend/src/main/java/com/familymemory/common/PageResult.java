package com.familymemory.common;

import lombok.Data;

/**
 * 分页结果
 */
@Data
public class PageResult<T> {

    private Long total;
    private Integer page;
    private Integer size;
    private java.util.List<T> list;

    public static <T> PageResult<T> of(Long total, Integer page, Integer size, java.util.List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setList(list);
        return result;
    }
}
