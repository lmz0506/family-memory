package com.familymemory.controller;

import com.familymemory.common.Result;
import com.familymemory.dto.req.CreateFamilyReq;
import com.familymemory.dto.req.CreateInviteCodeReq;
import com.familymemory.dto.req.JoinFamilyReq;
import com.familymemory.dto.resp.FamilyMemberResp;
import com.familymemory.dto.resp.FamilyStatisticsResp;
import com.familymemory.dto.resp.InviteCodeResp;
import com.familymemory.entity.Family;
import com.familymemory.service.FamilyService;
import com.familymemory.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 家庭控制器
 */
@RestController
@RequestMapping("/families")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;

    /**
     * 创建家庭
     */
    @PostMapping
    public Result<Family> createFamily(@Valid @RequestBody CreateFamilyReq req) {
        Long userId = UserContext.getUserId();
        return Result.success("创建成功", familyService.createFamily(req, userId));
    }

    /**
     * 获取家庭信息
     */
    @GetMapping("/{familyId}")
    public Result<Family> getFamily(@PathVariable Long familyId) {
        return Result.success(familyService.getFamilyById(familyId));
    }

    /**
     * 获取家庭成员列表
     */
    @GetMapping("/{familyId}/members")
    public Result<List<FamilyMemberResp>> getMembers(@PathVariable Long familyId) {
        return Result.success(familyService.getFamilyMembers(familyId));
    }

    /**
     * 生成邀请码
     */
    @PostMapping("/{familyId}/invite-codes")
    public Result<InviteCodeResp> createInviteCode(
            @PathVariable Long familyId,
            @RequestBody(required = false) CreateInviteCodeReq req) {
        Long userId = UserContext.getUserId();
        if (req == null) {
            req = new CreateInviteCodeReq();
        }
        return Result.success(familyService.createInviteCode(familyId, req, userId));
    }

    /**
     * 通过邀请码加入家庭
     */
    @PostMapping("/join")
    public Result<Family> joinFamily(@Valid @RequestBody JoinFamilyReq req) {
        Long userId = UserContext.getUserId();
        Family family = familyService.joinFamily(req, userId);
        return Result.success("加入成功", family);
    }

    /**
     * 获取家庭统计数据
     */
    @GetMapping("/{familyId}/statistics")
    public Result<FamilyStatisticsResp> getStatistics(@PathVariable Long familyId) {
        return Result.success(familyService.getStatistics(familyId));
    }
}
