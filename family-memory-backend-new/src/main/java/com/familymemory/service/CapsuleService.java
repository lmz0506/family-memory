package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.CapsuleRequest;
import com.familymemory.entity.TimeCapsule;
import com.familymemory.entity.User;
import com.familymemory.mapper.TimeCapsuleMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 珍藏封存与读取业务，索引只使用封存日期，正文受开启条件约束。
 */
@Service
public class CapsuleService {
    /**
     * 珍藏时光胶囊持久层，对应表：fm_time_capsule。
     */
    private final TimeCapsuleMapper mapper;
    /**
     * 用户账号持久层，对应表：fm_user。
     */
    private final UserMapper userMapper;
    /**
     * 家庭成员权限、家庭资料、邀请码及加入家庭业务。
     */
    private final FamilyService familyService;
    /**
     * 星海索引同事务写入服务。
     */
    private final UniverseIndexService universeIndexService;

    /**
     * 初始化 CapsuleService 的依赖。
     * @param m 珍藏时光胶囊持久层，对应表：fm_time_capsule
     * @param u 用户账号持久层，对应表：fm_user
     * @param f 家庭成员权限、家庭资料、邀请码及加入家庭业务
     * @param universeIndexService 星海索引同事务写入服务
     */
    public CapsuleService(TimeCapsuleMapper m, UserMapper u, FamilyService f, UniverseIndexService universeIndexService) {
        mapper = m;
        userMapper = u;
        familyService = f;
        this.universeIndexService = universeIndexService;
    }

    /**
     * 查询家庭珍藏的可公开元数据，不返回封存正文。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> list(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        return mapper.selectList(new LambdaQueryWrapper<TimeCapsule>().eq(TimeCapsule::getFamilyId, familyId).orderByAsc(TimeCapsule::getOpenDate)).stream().map(this::view).collect(Collectors.toList());
    }

    /**
     * 封存珍藏，并在同一事务按封存日期写入一条星海索引。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param req 珍藏封存请求，开启日期决定正文可读时间
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @Transactional
    public Map<String, Object> create(Long familyId, Long userId, CapsuleRequest req) {
        familyService.requireMember(familyId, userId);
        if (!req.getOpenDate().isAfter(LocalDate.now())) {
            throw BusinessException.badRequest("开启日期必须晚于今天");
        }
        TimeCapsule c = new TimeCapsule();
        c.setFamilyId(familyId);
        c.setCreatorId(userId);
        c.setRecipient(req.getRecipient());
        c.setTitle(req.getTitle().trim());
        c.setContent(req.getContent().trim());
        c.setCoverUrl(req.getCoverUrl());
        c.setOpenDate(req.getOpenDate());
        c.setIsOpened(0);
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        c.setDeleted(0);
        mapper.insert(c);
        universeIndexService.sync(familyId, "CAPSULE", c.getId(), c.getCreatedAt().toLocalDate(), c.getTitle());
        return view(c);
    }

    /**
     * 校验家庭权限，到开启日期后记录开启状态并返回正文。
     * @param id 业务ID，来源表：fm_time_capsule.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public Map<String, Object> detail(Long id, Long userId) {
        TimeCapsule c = mapper.selectById(id);
        if (c == null) {
            throw BusinessException.notFound("时光胶囊不存在");
        }
        familyService.requireMember(c.getFamilyId(), userId);
        if (!c.getOpenDate().isAfter(LocalDate.now()) && c.getIsOpened() == 0) {
            c.setIsOpened(1);
            c.setOpenedAt(LocalDateTime.now());
            mapper.updateById(c);
        }
        Map<String, Object> map = view(c);
        if (c.getIsOpened() == 1) {
            // 返回字段 content：正文内容。
            map.put("content", c.getContent());
        }
        return map;
    }

    /**
     * 修改本人珍藏的标题；封存日期、开启日期和正文保持不变。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param title 新标题
     */
    @Transactional
    public void updateTitle(Long capsuleId, Long userId, String title) {
        TimeCapsule capsule = requireOwned(capsuleId, userId);
        capsule.setTitle(title.trim());
        capsule.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(capsule);
        universeIndexService.sync(capsule.getFamilyId(), "CAPSULE", capsuleId, capsule.getCreatedAt().toLocalDate(), capsule.getTitle());
    }

    /**
     * 删除本人珍藏，同时逻辑删除星海索引，不调用会触发开启的详情方法。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @param userId 当前用户ID，来源表：fm_user.id
     */
    @Transactional
    public void delete(Long capsuleId, Long userId) {
        TimeCapsule capsule = requireOwned(capsuleId, userId);
        mapper.deleteById(capsuleId);
        universeIndexService.delete(capsule.getFamilyId(), "CAPSULE", capsuleId);
    }

    /**
     * 校验珍藏存在、家庭成员身份和创建者权限。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 当前用户可管理的未删除珍藏
     */
    private TimeCapsule requireOwned(Long capsuleId, Long userId) {
        TimeCapsule capsule = mapper.selectById(capsuleId);
        if (capsule == null) throw BusinessException.notFound("时光胶囊不存在");
        familyService.requireMember(capsule.getFamilyId(), userId);
        if (!capsule.getCreatorId().equals(userId)) throw BusinessException.forbidden("只能修改或删除自己创建的珍藏");
        return capsule;
    }

    /**
     * 组装珍藏元数据，按开启日期计算可读状态和剩余天数。
     * @param c 珍藏时光胶囊对象，来源表：fm_time_capsule
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> view(TimeCapsule c) {
        User user = userMapper.selectById(c.getCreatorId());
        boolean opened = c.getIsOpened() == 1 || !c.getOpenDate().isAfter(LocalDate.now());
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_time_capsule.id。
        map.put("id", c.getId());
        // 返回字段 title：业务标题。
        map.put("title", c.getTitle());
        // 返回字段 recipient：收信人称呼。
        map.put("recipient", c.getRecipient());
        // 返回字段 coverUrl：封面资源地址。
        map.put("coverUrl", c.getCoverUrl());
        // 返回字段 openDate：珍藏开启日期，不参与星海归档。
        map.put("openDate", c.getOpenDate());
        // 返回字段 opened：珍藏可读状态，true已达到开启条件，false尚未开启。
        map.put("opened", opened);
        // 返回字段 daysRemaining：距离开启或提醒日期的剩余天数，不小于0。
        map.put("daysRemaining", Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), c.getOpenDate())));
        // 返回字段 creatorName：创建者昵称，来源表：fm_user.nickname。
        map.put("creatorName", user == null ? "家人" : user.getNickname());
        // 返回字段 createdAt：业务记录创建时间。
        map.put("createdAt", c.getCreatedAt());
        return map;
    }
}
