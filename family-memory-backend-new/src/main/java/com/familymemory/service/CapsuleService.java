package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.CapsuleRequest;
import com.familymemory.entity.TimeCapsule;
import com.familymemory.entity.User;
import com.familymemory.mapper.TimeCapsuleMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CapsuleService {
    private final TimeCapsuleMapper mapper;
    private final UserMapper userMapper;
    private final FamilyService familyService;

    public CapsuleService(TimeCapsuleMapper m, UserMapper u, FamilyService f) {
        mapper = m;
        userMapper = u;
        familyService = f;
    }

    public List<Map<String, Object>> list(Long familyId, Long userId) {
        familyService.requireMember(familyId, userId);
        return mapper.selectList(new LambdaQueryWrapper<TimeCapsule>().eq(TimeCapsule::getFamilyId, familyId).orderByAsc(TimeCapsule::getOpenDate)).stream().map(this::view).collect(Collectors.toList());
    }

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
        return view(c);
    }

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
            map.put("content", c.getContent());
        }
        return map;
    }

    private Map<String, Object> view(TimeCapsule c) {
        User user = userMapper.selectById(c.getCreatorId());
        boolean opened = c.getIsOpened() == 1 || !c.getOpenDate().isAfter(LocalDate.now());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("title", c.getTitle());
        map.put("recipient", c.getRecipient());
        map.put("coverUrl", c.getCoverUrl());
        map.put("openDate", c.getOpenDate());
        map.put("opened", opened);
        map.put("daysRemaining", Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), c.getOpenDate())));
        map.put("creatorName", user == null ? "家人" : user.getNickname());
        map.put("createdAt", c.getCreatedAt());
        return map;
    }
}
