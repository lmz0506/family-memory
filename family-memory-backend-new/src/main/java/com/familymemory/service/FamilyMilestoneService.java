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

@Service
public class FamilyMilestoneService {
    private static final String PRIVATE = "PRIVATE";
    private static final String FAMILY = "FAMILY";
    private static final String SPECIFIC = "SPECIFIC";

    private final FamilyMilestoneMapper mapper;
    private final FamilyMilestoneViewerMapper viewerMapper;
    private final FamilyMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final FamilyService familyService;

    public FamilyMilestoneService(FamilyMilestoneMapper mapper, FamilyMilestoneViewerMapper viewerMapper,
                                  FamilyMemberMapper memberMapper, UserMapper userMapper,
                                  FamilyService familyService) {
        this.mapper = mapper;
        this.viewerMapper = viewerMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.familyService = familyService;
    }

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

    @Transactional
    public void delete(Long id, Long userId) {
        FamilyMilestone item = requireOwned(id, userId);
        viewerMapper.delete(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, id));
        mapper.deleteById(item.getId());
    }

    private FamilyMilestone requireOwned(Long id, Long userId) {
        FamilyMilestone item = mapper.selectById(id);
        if (item == null) throw BusinessException.notFound("家庭大事记不存在");
        familyService.requireMember(item.getFamilyId(), userId);
        if (!item.getCreatorId().equals(userId)) {
            throw BusinessException.forbidden("只有创建者可以修改或删除这条大事记");
        }
        return item;
    }

    private void apply(FamilyMilestone item, FamilyMilestoneRequest request, String scope) {
        item.setTitle(request.getTitle().trim());
        item.setDescription(trimToNull(request.getDescription()));
        item.setEventDate(request.getEventDate());
        item.setRecurringYearly(Boolean.TRUE.equals(request.getRecurringYearly()) ? 1 : 0);
        item.setVisibilityScope(scope);
    }

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

    private boolean canView(FamilyMilestone item, Long userId) {
        if (item.getCreatorId().equals(userId) || FAMILY.equals(item.getVisibilityScope())) return true;
        if (!SPECIFIC.equals(item.getVisibilityScope())) return false;
        return viewerMapper.selectCount(new LambdaQueryWrapper<FamilyMilestoneViewer>()
                .eq(FamilyMilestoneViewer::getMilestoneId, item.getId())
                .eq(FamilyMilestoneViewer::getUserId, userId)) > 0;
    }

    private Map<String, Object> view(FamilyMilestone item, Long userId) {
        User creator = userMapper.selectById(item.getCreatorId());
        LocalDate nextDate = nextOccurrence(item);
        boolean expired = item.getRecurringYearly() != 1 && item.getEventDate().isBefore(LocalDate.now());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.getId());
        map.put("familyId", item.getFamilyId());
        map.put("title", item.getTitle());
        map.put("description", item.getDescription());
        map.put("eventDate", item.getEventDate());
        map.put("nextEventDate", nextDate);
        map.put("recurringYearly", item.getRecurringYearly() == 1);
        map.put("visibilityScope", item.getVisibilityScope());
        map.put("visibleTo", visibleTo(item));
        map.put("daysRemaining", expired ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), nextDate));
        map.put("expired", expired);
        map.put("creatorId", item.getCreatorId());
        map.put("creatorName", creator == null ? "家庭成员" : creator.getNickname());
        map.put("canManage", item.getCreatorId().equals(userId));
        map.put("createdAt", item.getCreatedAt());
        return map;
    }

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
            data.put("userId", member.getUserId());
            data.put("displayName", member.getDisplayName());
            result.add(data);
        }
        return result;
    }

    private LocalDate nextOccurrence(FamilyMilestone item) {
        if (item.getRecurringYearly() != 1) return item.getEventDate();
        LocalDate today = LocalDate.now();
        LocalDate candidate = safeDate(today.getYear(), item.getEventDate());
        return candidate.isBefore(today) ? safeDate(today.getYear() + 1, item.getEventDate()) : candidate;
    }

    private LocalDate safeDate(int year, LocalDate source) {
        YearMonth month = YearMonth.of(year, source.getMonthValue());
        return month.atDay(Math.min(source.getDayOfMonth(), month.lengthOfMonth()));
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
