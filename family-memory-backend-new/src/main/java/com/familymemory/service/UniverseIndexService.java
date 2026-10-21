package com.familymemory.service;

import com.familymemory.entity.UniverseIndex;
import com.familymemory.entity.UniverseSourceType;
import com.familymemory.mapper.UniverseIndexMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Objects;

/** 业务写入侧的索引同步。必须加入发布、改标题、删除所在的同一事务。 */
@Service
public class UniverseIndexService {
    /** 星海索引持久层。 */
    private final UniverseIndexMapper mapper;

    /**
     * 初始化星海索引写入服务。
     * @param mapper 星海索引持久层
     */
    public UniverseIndexService(UniverseIndexMapper mapper) { this.mapper = mapper; }

    /**
     * 幂等同步一份已发布内容；不保存正文及开启状态。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏
     * @param sourceId 原业务ID，来源表：fm_memory.id / fm_time_capsule.id
     * @param date 记忆发生日期或珍藏封存日期，不得用当前日期代替
     * @param title 业务标题
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void sync(Long familyId, String sourceType, Long sourceId, LocalDate date, String title) {
        UniverseIndex item = new UniverseIndex();
        item.setFamilyId(Objects.requireNonNull(familyId, "家庭ID不能为空"));
        item.setSourceType(Objects.requireNonNull(UniverseSourceType.normalize(sourceType), "来源类型不能为空"));
        item.setSourceId(Objects.requireNonNull(sourceId, "来源ID不能为空"));
        item.setNodeDate(Objects.requireNonNull(date, "归档日期不能为空"));
        item.setTitle(Objects.requireNonNull(title, "标题不能为空"));
        mapper.upsert(item);
    }

    /**
     * 随原内容一起逻辑删除索引。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏
     * @param sourceId 原业务ID，来源表：fm_memory.id / fm_time_capsule.id
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void delete(Long familyId, String sourceType, Long sourceId) {
        mapper.softDelete(familyId, Objects.requireNonNull(UniverseSourceType.normalize(sourceType), "来源类型不能为空"), sourceId);
    }
}
