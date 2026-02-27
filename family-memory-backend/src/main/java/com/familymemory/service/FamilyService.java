package com.familymemory.service;

import com.familymemory.dto.req.CreateFamilyReq;
import com.familymemory.dto.req.CreateInviteCodeReq;
import com.familymemory.dto.req.JoinFamilyReq;
import com.familymemory.dto.resp.FamilyMemberResp;
import com.familymemory.dto.resp.FamilyStatisticsResp;
import com.familymemory.dto.resp.InviteCodeResp;
import com.familymemory.entity.Family;

import java.util.List;

/**
 * 家庭服务接口
 */
public interface FamilyService {

    /**
     * 创建家庭
     */
    Family createFamily(CreateFamilyReq req, Long userId);

    /**
     * 获取家庭信息
     */
    Family getFamilyById(Long familyId);

    /**
     * 获取家庭成员列表
     */
    List<FamilyMemberResp> getFamilyMembers(Long familyId);

    /**
     * 生成邀请码
     */
    InviteCodeResp createInviteCode(Long familyId, CreateInviteCodeReq req, Long userId);

    /**
     * 通过邀请码加入家庭
     */
    Family joinFamily(JoinFamilyReq req, Long userId);

    /**
     * 获取家庭统计数据
     */
    FamilyStatisticsResp getStatistics(Long familyId);

    /**
     * 获取用户当前家庭ID
     */
    Long getUserCurrentFamilyId(Long userId);
}
