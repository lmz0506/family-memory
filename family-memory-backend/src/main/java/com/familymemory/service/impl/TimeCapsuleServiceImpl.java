package com.familymemory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.familymemory.common.BusinessException;
import com.familymemory.dto.req.CreateCapsuleReq;
import com.familymemory.dto.resp.TimeCapsuleResp;
import com.familymemory.entity.FamilyMember;
import com.familymemory.entity.TimeCapsule;
import com.familymemory.entity.User;
import com.familymemory.mapper.FamilyMemberMapper;
import com.familymemory.mapper.TimeCapsuleMapper;
import com.familymemory.mapper.UserMapper;
import com.familymemory.service.TimeCapsuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 时光胶囊服务实现
 */
@Service
@RequiredArgsConstructor
public class TimeCapsuleServiceImpl implements TimeCapsuleService {

    private final TimeCapsuleMapper timeCapsuleMapper;
    private final UserMapper userMapper;
    private final FamilyMemberMapper familyMemberMapper;

    @Override
    public Map<String, Object> getCapsules(Long familyId, String status) {
        LambdaQueryWrapper<TimeCapsule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TimeCapsule::getFamilyId, familyId);

        if ("pending".equals(status)) {
            wrapper.eq(TimeCapsule::getIsOpened, 0);
        } else if ("opened".equals(status)) {
            wrapper.eq(TimeCapsule::getIsOpened, 1);
        }

        wrapper.orderByAsc(TimeCapsule::getOpenDate);
        List<TimeCapsule> capsules = timeCapsuleMapper.selectList(wrapper);

        // 计算待开启数量
        LambdaQueryWrapper<TimeCapsule> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(TimeCapsule::getFamilyId, familyId);
        pendingWrapper.eq(TimeCapsule::getIsOpened, 0);
        long pendingCount = timeCapsuleMapper.selectCount(pendingWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("pendingCount", pendingCount);
        result.put("list", capsules.stream().map(this::convertToResp).collect(Collectors.toList()));

        return result;
    }

    @Override
    public TimeCapsuleResp createCapsule(Long familyId, CreateCapsuleReq req, Long userId) {
        LocalDate openDate = LocalDate.parse(req.getOpenDate());

        if (!openDate.isAfter(LocalDate.now())) {
            throw new BusinessException("开启日期必须是未来的日期");
        }

        TimeCapsule capsule = new TimeCapsule();
        capsule.setFamilyId(familyId);
        capsule.setCreatorId(userId);
        capsule.setTitle(req.getTitle());
        capsule.setContent(req.getContent());
        capsule.setOpenDate(openDate);
        capsule.setIsOpened(0);
        timeCapsuleMapper.insert(capsule);

        return convertToResp(capsule);
    }

    @Override
    public TimeCapsuleResp getCapsuleById(Long capsuleId) {
        TimeCapsule capsule = timeCapsuleMapper.selectById(capsuleId);
        if (capsule == null) {
            throw new BusinessException(404, "胶囊不存在");
        }

        // 检查是否可以查看内容
        if (capsule.getIsOpened() == 0 && capsule.getOpenDate().isAfter(LocalDate.now())) {
            TimeCapsuleResp resp = new TimeCapsuleResp();
            resp.setId(capsule.getId());
            resp.setTitle(capsule.getTitle());
            resp.setContent(null); // 未开启时不返回内容
            resp.setOpenDate(capsule.getOpenDate().toString());
            resp.setIsOpened(false);
            return resp;
        }

        // 如果已到开启日期但未标记为已开启，则更新状态
        if (capsule.getIsOpened() == 0 && !capsule.getOpenDate().isAfter(LocalDate.now())) {
            capsule.setIsOpened(1);
            capsule.setOpenedAt(LocalDateTime.now());
            timeCapsuleMapper.updateById(capsule);
        }

        return convertToResp(capsule);
    }

    private TimeCapsuleResp convertToResp(TimeCapsule capsule) {
        TimeCapsuleResp resp = new TimeCapsuleResp();
        resp.setId(capsule.getId());
        resp.setTitle(capsule.getTitle());
        resp.setOpenDate(capsule.getOpenDate().toString());
        resp.setIsOpened(capsule.getIsOpened() == 1);

        // 只有已开启才返回内容
        if (capsule.getIsOpened() == 1 || !capsule.getOpenDate().isAfter(LocalDate.now())) {
            resp.setContent(capsule.getContent());
        }

        if (capsule.getOpenedAt() != null) {
            resp.setOpenedAt(capsule.getOpenedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        if (capsule.getCreatedAt() != null) {
            resp.setCreatedAt(capsule.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        // 获取创建者信息
        User creator = userMapper.selectById(capsule.getCreatorId());
        if (creator != null) {
            resp.setCreatorAvatar(creator.getAvatar());

            // 获取在家庭中的昵称
            LambdaQueryWrapper<FamilyMember> memberWrapper = new LambdaQueryWrapper<>();
            memberWrapper.eq(FamilyMember::getFamilyId, capsule.getFamilyId());
            memberWrapper.eq(FamilyMember::getUserId, capsule.getCreatorId());
            FamilyMember member = familyMemberMapper.selectOne(memberWrapper);
            resp.setCreatorName(member != null ? member.getNickname() : creator.getNickname());
        }

        return resp;
    }
}
