package com.familymemory.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.common.FileUrlHelper;
import com.familymemory.dto.req.CreateFamilyReq;
import com.familymemory.dto.req.CreateInviteCodeReq;
import com.familymemory.dto.req.JoinFamilyReq;
import com.familymemory.dto.resp.FamilyMemberResp;
import com.familymemory.dto.resp.FamilyStatisticsResp;
import com.familymemory.dto.resp.InviteCodeResp;
import com.familymemory.entity.*;
import com.familymemory.mapper.*;
import com.familymemory.service.FamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 家庭服务实现
 */
@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final UserMapper userMapper;
    private final InviteCodeMapper inviteCodeMapper;
    private final MemoryMapper memoryMapper;
    private final MemoryMediaMapper memoryMediaMapper;
    private final TimeCapsuleMapper timeCapsuleMapper;

    @Override
    @Transactional
    public Family createFamily(CreateFamilyReq req, Long userId) {
        // 创建家庭
        Family family = new Family();
        family.setName(req.getName());
        family.setDescription(req.getDescription());
        family.setCreatorId(userId);
        familyMapper.insert(family);

        // 添加创建者为管理员成员
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(family.getId());
        member.setUserId(userId);
        member.setRole("创建者");
        member.setNickname(user.getNickname());
        member.setColor("bg-blue-100");
        member.setIsAdmin(1);
        member.setJoinedAt(LocalDateTime.now());
        familyMemberMapper.insert(member);

        return family;
    }

    @Override
    public Family getFamilyById(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException(404, "家庭不存在");
        }
        return family;
    }

    @Override
    public List<FamilyMemberResp> getFamilyMembers(Long familyId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getFamilyId, familyId);
        List<FamilyMember> members = familyMemberMapper.selectList(wrapper);

        return members.stream().map(member -> {
            User user = userMapper.selectById(member.getUserId());
            FamilyMemberResp resp = new FamilyMemberResp();
            resp.setId(member.getId());
            resp.setUserId(member.getUserId());
            resp.setNickname(member.getNickname());
            resp.setRole(member.getRole());
            resp.setAvatar(user != null ? FileUrlHelper.toFullUrl(user.getAvatar()) : "👤");
            resp.setColor(member.getColor());
            resp.setIsAdmin(member.getIsAdmin() == 1);
            return resp;
        }).collect(Collectors.toList());
    }

    @Override
    public InviteCodeResp createInviteCode(Long familyId, CreateInviteCodeReq req, Long userId) {
        // 生成邀请码
        String code = RandomUtil.randomString("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 8);

        InviteCode inviteCode = new InviteCode();
        inviteCode.setFamilyId(familyId);
        inviteCode.setCreatorId(userId);
        inviteCode.setCode(code);
        inviteCode.setExpireAt(LocalDateTime.now().plusHours(req.getExpireHours()));
        inviteCode.setMaxUses(req.getMaxUses());
        inviteCode.setUsedCount(0);
        inviteCode.setStatus(1);
        inviteCodeMapper.insert(inviteCode);

        InviteCodeResp resp = new InviteCodeResp();
        resp.setCode(code);
        resp.setExpireAt(inviteCode.getExpireAt().toString());
        resp.setInviteLink("https://family-memory.app/join/" + code);
        return resp;
    }

    @Override
    @Transactional
    public Family joinFamily(JoinFamilyReq req, Long userId) {
        // 查找邀请码
        LambdaQueryWrapper<InviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InviteCode::getCode, req.getInviteCode());
        wrapper.eq(InviteCode::getStatus, 1);
        InviteCode inviteCode = inviteCodeMapper.selectOne(wrapper);

        if (inviteCode == null) {
            throw new BusinessException("邀请码无效");
        }

        if (inviteCode.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("邀请码已过期");
        }

        if (inviteCode.getUsedCount() >= inviteCode.getMaxUses()) {
            throw new BusinessException("邀请码已达使用上限");
        }

        // 检查是否已是成员
        LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(FamilyMember::getFamilyId, inviteCode.getFamilyId());
        memberWrapper.eq(FamilyMember::getUserId, userId);
        if (familyMemberMapper.selectCount(memberWrapper) > 0) {
            throw new BusinessException("您已是该家庭成员");
        }

        // 添加成员
        User user = userMapper.selectById(userId);
        FamilyMember member = new FamilyMember();
        member.setFamilyId(inviteCode.getFamilyId());
        member.setUserId(userId);
        member.setNickname(req.getNickname());
        member.setRole(req.getRole() != null ? req.getRole() : "成员");
        member.setColor("bg-gray-100");
        member.setIsAdmin(0);
        member.setJoinedAt(LocalDateTime.now());
        familyMemberMapper.insert(member);

        // 更新邀请码使用次数
        inviteCode.setUsedCount(inviteCode.getUsedCount() + 1);
        inviteCodeMapper.updateById(inviteCode);

        return familyMapper.selectById(inviteCode.getFamilyId());
    }

    @Override
    public FamilyStatisticsResp getStatistics(Long familyId) {
        FamilyStatisticsResp resp = new FamilyStatisticsResp();

        // 成员数量
        LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
        memberWrapper.eq(FamilyMember::getFamilyId, familyId);
        resp.setMemberCount(familyMemberMapper.selectCount(memberWrapper));

        // 记忆数量
        LambdaQueryWrapper<Memory> memoryWrapper = new LambdaQueryWrapper<>();
        memoryWrapper.eq(Memory::getFamilyId, familyId);
        resp.setMemoryCount(memoryMapper.selectCount(memoryWrapper));

        // 媒体数量
        LambdaQueryWrapper<MemoryMedia> mediaWrapper = new LambdaQueryWrapper<>();
        mediaWrapper.inSql(MemoryMedia::getMemoryId, "SELECT id FROM fm_memory WHERE family_id = " + familyId);
        resp.setPhotoCount(memoryMediaMapper.selectCount(mediaWrapper));

        // 待开启胶囊数量
        LambdaQueryWrapper<TimeCapsule> capsuleWrapper = new LambdaQueryWrapper<>();
        capsuleWrapper.eq(TimeCapsule::getFamilyId, familyId);
        capsuleWrapper.eq(TimeCapsule::getIsOpened, 0);
        resp.setCapsuleCount(timeCapsuleMapper.selectCount(capsuleWrapper));

        // 记录天数（从家庭创建到现在）
        Family family = familyMapper.selectById(familyId);
        if (family != null && family.getCreatedAt() != null) {
            long days = ChronoUnit.DAYS.between(family.getCreatedAt().toLocalDate(), LocalDateTime.now().toLocalDate());
            resp.setRecordDays(days + 1);
        } else {
            resp.setRecordDays(1L);
        }

        return resp;
    }

    @Override
    public Long getUserCurrentFamilyId(Long userId) {
        LambdaQueryWrapper<FamilyMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FamilyMember::getUserId, userId);
        wrapper.last("LIMIT 1");
        FamilyMember member = familyMemberMapper.selectOne(wrapper);
        return member != null ? member.getFamilyId() : null;
    }
}
