package com.familymemory.entity;

import com.familymemory.common.BusinessException;
import java.util.Locale;

/** 星海索引来源类型，对应 fm_universe_index.source_type。 */
public enum UniverseSourceType {
    /** MEMORY 记忆；来源ID对应 fm_memory.id。 */
    MEMORY,
    /** CAPSULE 珍藏；来源ID对应 fm_time_capsule.id。 */
    CAPSULE;

    /**
     * 校验并规范化来源类型，供读写业务共同使用。
     * @param value MEMORY 记忆，CAPSULE 珍藏；空值在查询时表示全部
     * @return 规范化的大写枚举名称，空值返回null
     */
    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)).name(); }
        catch (IllegalArgumentException exception) { throw BusinessException.badRequest("内容来源仅支持 MEMORY 或 CAPSULE"); }
    }
}
