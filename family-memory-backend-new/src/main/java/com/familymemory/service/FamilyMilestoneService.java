package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.FamilyMilestoneRequest;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.FamilyMilestone;
import com.familymemory.entity.FamilyMilestoneViewer;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.FamilyMilestoneMapper;
import com.familymemory.mapper.FamilyMilestoneViewerMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 家庭提醒业务，处理可见范围、指定成员及年度重复日期。
 */
@Service
public class FamilyMilestoneService {
    /**
     * 仅创建者可见的范围值：PRIVATE。
     */
    private static final String PRIVATE = "PRIVATE";
    /**
     * 全体家庭成员可见的范围值：FAMILY。
     */
    private static final String FAMILY = "FAMILY";
    /**
     * 指定家庭成员可见的范围值：SPECIFIC。
     */
    private static final String SPECIFIC = "SPECIFIC";

    /** 家庭大事记提醒持久层，对应表：fm_family_milestone。 */
    private final FamilyMilestoneMapper mapper;
    /**
     * 家庭大事记指定可见成员持久层，对应表：fm_family_milestone_viewer。
     */
    private final FamilyMilestoneViewerMapper viewerMapper;
    /**
     * 家庭成员关系持久层，对应表：fm_family_member。
     */
    private final FamilyMemberMapper memberMapper;
    /**
     * 用户账号持久层，对应表：fm_user。
     */
    private final UserMapper userMapper;
    /**
     * 家庭成员权限、家庭资料、邀请码及加入家庭业务。
     */
    private final FamilyService familyService;

    /**
     * 初始化 FamilyMilestoneService 的依赖。
     * @param mapper 家庭大事记提醒持久层，对应表：fm_family_milestone
     * @param viewerMapper 家庭大事记指定可见成员持久层，对应表：fm_family_milestone_viewer
     * @param memberMapper 家庭成员关系持久层，对应表：fm_family_member
     * @param userMapper 用户账号持久层，对应表：fm_user
     * @param familyService 家庭成员权限、家庭资料、邀请码及加入家庭业务
     */
    public FamilyMilestoneService(FamilyMilestoneMapper mapper, FamilyMilestoneViewerMapper viewerMapper,
                                  FamilyMemberMapper memberMapper, UserMapper userMapper,
                                  FamilyService familyService) {
        this.mapper = mapper;
        this.viewerMapper = viewerMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.familyService = familyService;
    }

    /**
     * 查询当前用户可见提醒并按到期状态及下次日期排序。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    public List<Map<String, Object>> list(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        List<FamilyMilestone> milestones = mapper.selectList(new LambdaQueryWrapper<FamilyMilestone>()
                .eq(FamilyMilestone::getFamilyId, familyId));
        List<Map<String, Object>> result = milestones.stream()
                .filter(item -> canView(item, userId))
                .map(item -> view(item, userId))
                .collect(Collectors.toList());
        result.sort(Comparator
                .comparing((Map<String, Object> item) -> (Boolean) item.get("expired"))
                .thenComparing(item -> (LocalDate) item.get("nextEventDate")));
        return result;
    }

    /**
     * 创建提醒并保存指定可见成员。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param request 家庭提醒创建或修改请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @Transactional
    public Map<String, Object> create(Long familyId, Long userId, FamilyMilestoneRequest request) {
        familyService.requireMember(familyId, userId);
        String scope = validateRequest(familyId, userId, request);
        FamilyMilestone item = new FamilyMilestone();
        item.setFamilyId(familyId);
        item.setCreatorId(userId);
        apply(item, request, scope);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setDeleted(0);
        mapper.insert(item);
        saveViewers(item.getId(), userId, scope, request.getViewerUserIds());
        return view(item, userId);
    }

    /**
     * 仅允许创建者修改提醒及可见成员。
     * @param id 业务ID，来源表：fm_family_milestone.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @param request 家庭提醒创建或修改请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @Transactional
    public Map<String, Object> update(Long id, Long userId, FamilyMilestoneRequest request) {
        FamilyMilestone item = requireOwned(id, userId);
        String scope = validateRequest(item.getFamilyId(), userId, request);
        apply(item, request, scope);
        item.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(item);
        viewerMapper.delete(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, id));
        saveViewers(id, userId, scope, request.getViewerUserIds());
        return view(item, userId);
    }

    /**
     * 删除本人提醒及指定可见成员关系。
     * @param id 业务ID，来源表：fm_family_milestone.id
     * @param userId 当前用户ID，来源表：fm_user.id
     */
    @Transactional
    public void delete(Long id, Long userId) {
        FamilyMilestone item = requireOwned(id, userId);
        viewerMapper.delete(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, id));
        mapper.deleteById(item.getId());
    }

    /**
     * 校验提醒存在、家庭成员身份和创建者权限。
     * @param id 业务ID，来源表：fm_family_milestone.id
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 已通过创建者权限校验的业务记录
     */
    private FamilyMilestone requireOwned(Long id, Long userId) {
        FamilyMilestone item = mapper.selectById(id);
        if (item == null) throw BusinessException.notFound("家庭大事记不存在");
        familyService.requireMember(item.getFamilyId(), userId);
        if (!item.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("只有创建者可以修改或删除这条大事记");
        }
        return item;
    }

    /**
     * 将请求中的标题、说明、日期和范围写入提醒对象。
     * @param item 家庭大事记提醒对象，来源表：fm_family_milestone
     * @param request 家庭提醒创建或修改请求
     * @param scope 可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员
     */
    private void apply(FamilyMilestone item, FamilyMilestoneRequest request, String scope) {
        item.setTitle(request.getTitle().trim());
        item.setDescription(trimToNull(request.getDescription()));
        item.setEventDate(request.getEventDate());
        item.setRecurringYearly(Boolean.TRUE.equals(request.getRecurringYearly()) ? 1 : 0);
        item.setVisibilityScope(scope);
    }

    /**
     * 校验重复规则、日期与指定成员，返回规范化可见范围。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param creatorId 创建者ID，来源表：fm_user.id
     * @param request 家庭提醒创建或修改请求
     * @return 规范化可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员
     */
    private String validateRequest(Long familyId, Long creatorId, FamilyMilestoneRequest request) {
        String scope = request.getVisibilityScope() == null ? PRIVATE :
                request.getVisibilityScope().trim().toUpperCase(Locale.ROOT);
        if (!PRIVATE.equals(scope) && !FAMILY.equals(scope) && !SPECIFIC.equals(scope)) {
            throw BusinessException.badRequest("可见范围不正确");
        }
        if (!Boolean.TRUE.equals(request.getRecurringYearly()) && request.getEventDate().isBefore(LocalDate.now())) {
            throw BusinessException.badRequest("一次性提醒日期不能早于今天");
        }
        if (SPECIFIC.equals(scope)) {
            Set<Long> requested = request.getViewerUserIds() == null ? new HashSet<>() :
                    new HashSet<>(request.getViewerUserIds());
            requested.removeIf(id -> id == null || id.equals(creatorId));
            if (requested.isEmpty()) throw BusinessException.badRequest("请至少选择一位可见家人");
            long memberCount = memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>()
                    .eq(FamilyMember::getFamilyId, familyId)
                    .in(FamilyMember::getUserId, requested));
            if (memberCount != requested.size()) throw BusinessException.badRequest("指定人员必须是当前家庭成员");
        }
        return scope;
    }

    /**
     * 保存去重后的指定可见用户，排除创建者本人。
     * @param milestoneId 大事记ID，来源表：fm_family_milestone.id
     * @param creatorId 创建者ID，来源表：fm_user.id
     * @param scope 可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员
     * @param viewerUserIds 指定可见用户ID列表，来源表：fm_user.id
     */
    private void saveViewers(Long milestoneId, Long creatorId, String scope, List<Long> viewerUserIds) {
        if (!SPECIFIC.equals(scope) || viewerUserIds == null) return;
        Set<Long> ids = new HashSet<>(viewerUserIds);
        ids.remove(creatorId);
        for (Long viewerId : ids) {
            FamilyMilestoneViewer viewer = new FamilyMilestoneViewer();
            viewer.setMilestoneId(milestoneId);
            viewer.setUserId(viewerId);
            viewer.setCreatedAt(LocalDateTime.now());
            viewerMapper.insert(viewer);
        }
    }

    /**
     * 判断当前用户是否有权查看提醒。
     * @param item 家庭大事记提醒对象，来源表：fm_family_milestone
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return true允许查看，false不可查看
     */
    private boolean canView(FamilyMilestone item, Long userId) {
        if (item.getCreatorId().equals(userId) || FAMILY.equals(item.getVisibilityScope())) return true;
        if (!SPECIFIC.equals(item.getVisibilityScope())) return false;
        return viewerMapper.selectCount(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, item.getId())
                .eq(FamilyMilestoneViewer::getUserId, userId)) > 0;
    }

    /**
     * 组装提醒展示字段和当前用户管理权限。
     * @param item 家庭大事记提醒对象，来源表：fm_family_milestone
     * @param userId 当前用户ID，来源表：fm_user.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private Map<String, Object> view(FamilyMilestone item, Long userId) {
        User creator = userMapper.selectById(item.getCreatorId());
        LocalDate nextDate = nextOccurrence(item);
        boolean expired = item.getRecurringYearly() != 1 && item.getEventDate().isBefore(LocalDate.now());
        Map<String, Object> map = new LinkedHashMap<>();
        // 返回字段 id：记录ID，来源表：fm_family_milestone.id。
        map.put("id", item.getId());
        // 返回字段 familyId：家庭ID，来源表：fm_family.id。
        map.put("familyId", item.getFamilyId());
        // 返回字段 title：业务标题。
        map.put("title", item.getTitle());
        // 返回字段 description：大事记提醒说明，来源表：fm_family_milestone.description。
        map.put("description", item.getDescription());
        // 返回字段 eventDate：提醒原始日期。
        map.put("eventDate", item.getEventDate());
        // 返回字段 nextEventDate：下一次提醒日期。
        map.put("nextEventDate", nextDate);
        // 返回字段 recurringYearly：是否每年重复，true每年，false一次性。
        map.put("recurringYearly", item.getRecurringYearly() == 1);
        // 返回字段 visibilityScope：可见范围，PRIVATE仅创建者，FAMILY全体家人，SPECIFIC指定成员。
        map.put("visibilityScope", item.getVisibilityScope());
        // 返回字段 visibleTo：指定可见家庭成员列表。
        map.put("visibleTo", visibleTo(item));
        // 返回字段 daysRemaining：距离开启或提醒日期的剩余天数，不小于0。
        map.put("daysRemaining", expired ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), nextDate));
        // 返回字段 expired：一次性提醒是否过期，true已过期，false未过期或每年重复。
        map.put("expired", expired);
        // 返回字段 creatorId：创建者ID，来源表：fm_user.id。
        map.put("creatorId", item.getCreatorId());
        // 返回字段 creatorName：创建者昵称，来源表：fm_user.nickname。
        map.put("creatorName", creator == null ? "家庭成员" : creator.getNickname());
        // 返回字段 canManage：当前用户是否为创建者，true可管理，false不可管理。
        map.put("canManage", item.getCreatorId().equals(userId));
        // 返回字段 createdAt：业务记录创建时间。
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

    /**
     * 查询提醒的指定可见成员公开资料。
     * @param item 家庭大事记提醒对象，来源表：fm_family_milestone
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    private List<Map<String, Object>> visibleTo(FamilyMilestone item) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!SPECIFIC.equals(item.getVisibilityScope())) return result;
        List<FamilyMilestoneViewer> viewers = viewerMapper.selectList(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, item.getId()));
        for (FamilyMilestoneViewer viewer : viewers) {
            FamilyMember member = memberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>()
                    .eq(FamilyMember::getFamilyId, item.getFamilyId())
                    .eq(FamilyMember::getUserId, viewer.getUserId()));
            if (member == null) continue;
            Map<String, Object> data = new LinkedHashMap<>();
            // 返回字段 userId：用户ID，来源表：fm_user.id。
            data.put("userId", member.getUserId());
            // 返回字段 displayName：家庭内称呼。
            data.put("displayName", member.getDisplayName());
            result.add(data);
        }
        return result;
    }

    /**
     * 计算提醒下一次发生日期。
     * @param item 家庭大事记提醒对象，来源表：fm_family_milestone
     * @return 下一次提醒日期
     */
    private LocalDate nextOccurrence(FamilyMilestone item) {
        if (item.getRecurringYearly() != 1) return item.getEventDate();
        LocalDate today = LocalDate.now();
        LocalDate candidate = safeDate(today.getYear(), item.getEventDate());
        return candidate.isBefore(today) ? safeDate(today.getYear() + 1, item.getEventDate()) : candidate;
    }

    /**
     * 构造指定年份日期，闰日超出目标月时取月末。
     * @param year 目标年份
     * @param source 提醒原始日期，用于提取月日
     * @return 目标年份内有效的日期
     */
    private LocalDate safeDate(int year, LocalDate source) {
        YearMonth month = YearMonth.of(year, source.getMonthValue());
        return month.atDay(Math.min(source.getDayOfMonth(), month.lengthOfMonth()));
    }

    /**
     * 去除首尾空白，空文本转为null。
     * @param value 待规范化文本
     * @return 去除空白后的文本或null
     */
    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
