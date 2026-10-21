package com.familymemory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.familymemory.entity.UniverseIndex;
import com.familymemory.dto.UniverseDayCount;
import com.familymemory.dto.UniverseItemResponse;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;

/** 星海索引持久层。读取必须限定家庭及有效状态，不访问正文或媒体表。 */
@Mapper
public interface UniverseIndexMapper extends BaseMapper<UniverseIndex> {
    /** 共用查询条件；日期、来源和标题条件由已校验参数控制。 */
    String FILTER = " FROM fm_universe_index i INNER JOIN fm_family f ON f.id=i.family_id AND f.deleted=0"
        + " WHERE i.family_id=#{familyId} AND i.deleted=0"
        + " <if test='date != null'>AND i.node_date=#{date}</if>"
        + " <if test='sourceType != null'>AND i.source_type=#{sourceType}</if>"
        + " <if test='pattern != null'>AND i.title LIKE #{pattern} ESCAPE '!'</if>";

    /**
     * 查询有内容日期及两类内容数量。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 按归档日期升序的计数行
     */
    @Select("SELECT i.node_date, SUM(CASE WHEN i.source_type='MEMORY' THEN 1 ELSE 0 END) memory_count,"
        + " SUM(CASE WHEN i.source_type='CAPSULE' THEN 1 ELSE 0 END) capsule_count"
        + " FROM fm_universe_index i INNER JOIN fm_family f ON f.id=i.family_id AND f.deleted=0"
        + " WHERE i.family_id=#{familyId} AND i.deleted=0 GROUP BY i.node_date ORDER BY i.node_date")
    List<UniverseDayCount> dailyCounts(@Param("familyId") Long familyId);

    /**
     * 按年份范围和索引ID游标读取下一批，避免深分页及重复统计总数。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param start 年份起始日，包含
     * @param end 下一年起始日，不包含
     * @param afterId 排他索引游标，来源表：fm_universe_index.id，首次为0
     * @param limit 本次读取上限，为业务批量大小加1，用于判断下一批
     * @return 按索引ID升序的轻量条目，不包含正文
     */
    @Select("SELECT i.id AS index_id,i.source_type,i.source_id,i.node_date,i.title"
        + " FROM fm_universe_index i INNER JOIN fm_family f ON f.id=i.family_id AND f.deleted=0"
        + " WHERE i.family_id=#{familyId} AND i.deleted=0 AND i.node_date>=#{start}"
        + " AND i.node_date<#{end} AND i.id>#{afterId} ORDER BY i.id ASC LIMIT #{limit}")
    List<UniverseItemResponse> yearBatch(@Param("familyId") Long familyId, @Param("start") LocalDate start,
        @Param("end") LocalDate end, @Param("afterId") long afterId, @Param("limit") int limit);

    /**
     * 统计日期列表或标题搜索的总数。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param date 精确归档日期；null 表示不限日期
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；null 表示全部
     * @param pattern 已用感叹号转义的 LIKE 模式；null 表示不过滤标题
     * @return 有效索引条数
     */
    @Select("<script>SELECT COUNT(*)" + FILTER + "</script>")
    long countItems(@Param("familyId") Long familyId, @Param("date") LocalDate date,
                    @Param("sourceType") String sourceType, @Param("pattern") String pattern);

    /**
     * 有界分页查询轻量内容；日期内按索引ID稳定排序，搜索按日期降序。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param date 归档日期；null 表示标题搜索
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；null 表示全部
     * @param pattern 已转义的标题 LIKE 模式；null 表示不限制标题
     * @param offset 跳过条数，从0开始
     * @param size 本页上限，由业务层限制为1至50
     * @return 仅包含定位字段和标题的条目
     */
    @Select("<script>SELECT i.id AS index_id,i.source_type,i.source_id,i.node_date,i.title" + FILTER
        + " ORDER BY i.node_date DESC,i.id ASC LIMIT #{size} OFFSET #{offset}</script>")
    List<UniverseItemResponse> listItems(@Param("familyId") Long familyId, @Param("date") LocalDate date,
        @Param("sourceType") String sourceType, @Param("pattern") String pattern,
        @Param("offset") long offset, @Param("size") int size);

    /**
     * 发布或改标题时幂等同步；已有索引的归档日期不允许随标题更新而改变。
     * @param item 已取得真实业务主键的索引值
     * @return 受影响行数
     */
    @Insert("INSERT INTO fm_universe_index(family_id,source_type,source_id,node_date,title,deleted)"
        + " VALUES(#{familyId},#{sourceType},#{sourceId},#{nodeDate},#{title},0)"
        + " ON DUPLICATE KEY UPDATE family_id=VALUES(family_id),title=VALUES(title),deleted=0")
    int upsert(UniverseIndex item);

    /**
     * 逻辑删除原内容对应的唯一索引。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏
     * @param sourceId 来源ID，fm_memory.id 或 fm_time_capsule.id
     * @return 受影响行数
     */
    @Update("UPDATE fm_universe_index SET deleted=1 WHERE family_id=#{familyId}"
        + " AND source_type=#{sourceType} AND source_id=#{sourceId} AND deleted=0")
    int softDelete(@Param("familyId") Long familyId, @Param("sourceType") String sourceType,
                   @Param("sourceId") Long sourceId);
}
