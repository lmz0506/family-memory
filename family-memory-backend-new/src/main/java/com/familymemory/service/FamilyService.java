package com.familymemory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.FamilyRequest;
import com.familymemory.dto.InviteRequest;
import com.familymemory.dto.JoinFamilyRequest;
import com.familymemory.entity.Family;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.InviteCode;
import com.familymemory.entity.Memory;
import com.familymemory.entity.MemoryMedia;
import com.familymemory.entity.TimeCapsule;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMapper;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.InviteCodeMapper;
import com.familymemory.mapper.MemoryMapper;
import com.familymemory.mapper.MemoryMediaMapper;
import com.familymemory.mapper.TimeCapsuleMapper;
import com.familymemory.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class FamilyService {
    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper memberMapper;
    private final UserMapper userMapper;
    private final InviteCodeMapper inviteMapper;
    private final MemoryMapper memoryMapper;
    private final MemoryMediaMapper mediaMapper;
    private final TimeCapsuleMapper capsuleMapper;

    public FamilyService(FamilyMapper f, FamilyMemberMapper m, UserMapper u, InviteCodeMapper i, MemoryMapper mm,
                         MemoryMediaMapper media, TimeCapsuleMapper c) {
        familyMapper = f;
        memberMapper = m;
        userMapper = u;
        inviteMapper = i;
        memoryMapper = mm;
        mediaMapper = media;
        capsuleMapper = c;
    }

    public FamilyMember requireMember(Long familyId, Long userId) {
        FamilyMember member =
                memberMapper.selectOne(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId, familyId).eq(FamilyMember::getUserId, userId));
        if (member == null) throw BusinessException.forbidden("你不是该家庭成员");
        return member;
    }

    @Transactional
    public Map<String, Object> create(FamilyRequest req, Long userId) {
        Family family = new Family();
        family.setName(req.getName().trim());
        family.setDescription(req.getDescription());
        family.setCoverUrl(req.getCoverUrl());
        family.setCreatorId(userId);
        family.setCreatedAt(LocalDateTime.now());
        family.setUpdatedAt(LocalDateTime.now());
        family.setDeleted(0);
        familyMapper.insert(family);
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole("家庭管理员");
        member.setDisplayName(user.getNickname());
        member.setColor("peach");
        member.setIsAdmin(1);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        member.setDeleted(0);
        memberMapper.insert(member);
        return detail(family.getId(), userId);
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember member :
                memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getUserId, userId))) {
            result.add(detail(member.getFamilyId(), userId));
        }
        return result;
    }

    public Map<String, Object> detail(Long familyId, Long userId) {
        requireMember(familyId, userId);
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw BusinessException.notFound("家庭不存在");
        }
        long memberCount =
                memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                        familyId));
        long memoryCount = memoryMapper.selectCount(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId, familyId));
        long photoCount = 0;
        List<Memory> memories = memoryMapper.selectList(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                familyId).select(Memory::getId));
        if (!memories.isEmpty()) {
            photoCount = mediaMapper.selectCount(new LambdaQueryWrapper<MemoryMedia>().in(MemoryMedia::getMemoryId,
                    memories.stream().map(Memory::getId).toArray()));
        }
        long capsuleCount =
                capsuleMapper.selectCount(new LambdaQueryWrapper<TimeCapsule>().eq(TimeCapsule::getFamilyId,
                        familyId).eq(TimeCapsule::getIsOpened, 0));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", family.getId());
        map.put("name", family.getName());
        map.put("description", family.getDescription());
        map.put("coverUrl", family.getCoverUrl());
        map.put("createdAt", family.getCreatedAt());
        map.put("memberCount", memberCount);
        map.put("memoryCount", memoryCount);
        map.put("photoCount", photoCount);
        map.put("capsuleCount", capsuleCount);
        map.put("recordDays", family.getCreatedAt() == null ? 0 :
                ChronoUnit.DAYS.between(family.getCreatedAt().toLocalDate(), LocalDate.now()) + 1);
        return map;
    }

    public List<Map<String, Object>> members(Long familyId, Long userId) {
        requireMember(familyId, userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FamilyMember member :
                memberMapper.selectList(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                        familyId).orderByAsc(FamilyMember::getJoinedAt))) {
            User user = userMapper.selectById(member.getUserId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", member.getId());
            map.put("userId", member.getUserId());
            map.put("displayName", member.getDisplayName());
            map.put("role", member.getRole());
            map.put("color", member.getColor());
            map.put("admin", member.getIsAdmin() == 1);
            map.put("avatar", user == null ? null : user.getAvatar());
            map.put("memoryCount", memoryMapper.selectCount(new LambdaQueryWrapper<Memory>().eq(Memory::getFamilyId,
                    familyId).eq(Memory::getAuthorId, member.getUserId())));
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> invite(Long familyId, InviteRequest req, Long userId) {
        FamilyMember member = requireMember(familyId, userId);
        if (member.getIsAdmin() != 1) throw BusinessException.forbidden("只有管理员可以邀请成员");
        InviteCode code = new InviteCode();
        code.setFamilyId(familyId);
        code.setCreatorId(userId);
        code.setCode(randomCode());
        code.setExpireAt(LocalDateTime.now().plusHours(Math.max(1, req.getExpireHours())));
        code.setMaxUses(Math.max(1, req.getMaxUses()));
        code.setUsedCount(0);
        code.setStatus(1);
        code.setCreatedAt(LocalDateTime.now());
        inviteMapper.insert(code);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code.getCode());
        map.put("expireAt", code.getExpireAt());
        map.put("maxUses", code.getMaxUses());
        return map;
    }

    @Transactional
    public Map<String, Object> join(JoinFamilyRequest req, Long userId) {
        InviteCode code = inviteMapper.selectOne(new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode,
                req.getInviteCode().trim().toUpperCase()));
        if (code == null || code.getStatus() != 1 || code.getExpireAt().isBefore(LocalDateTime.now()) || code.getUsedCount() >= code.getMaxUses())
            throw BusinessException.badRequest("邀请码无效或已过期");
        if (memberMapper.selectCount(new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getFamilyId,
                code.getFamilyId()).eq(FamilyMember::getUserId, userId)) > 0)
            throw BusinessException.badRequest("你已经在这个家庭中");
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(code.getFamilyId());
        member.setUserId(userId);
        member.setRole(req.getRole() == null ? "家庭成员" : req.getRole());
        member.setDisplayName(req.getDisplayName() == null || req.getDisplayName().trim().isEmpty() ?
                user.getNickname() : req.getDisplayName());
        member.setColor("sky");
        member.setIsAdmin(0);
        member.setJoinedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        member.setDeleted(0);
        memberMapper.insert(member);
        code.setUsedCount(code.getUsedCount() + 1);
        if (code.getUsedCount() >= code.getMaxUses()) code.setStatus(0);
        inviteMapper.updateById(code);
        return detail(code.getFamilyId(), userId);
    }

    private String randomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder s = new StringBuilder();
        Random r = new Random();
        for (int i = 0; i < 8; i++) s.append(chars.charAt(r.nextInt(chars.length())));
        return s.toString();
    }
}
