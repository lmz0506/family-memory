package com.familymemory.controller;

import com.familymemory.common.Result;
import com.familymemory.dto.req.CreateCapsuleReq;
import com.familymemory.dto.resp.TimeCapsuleResp;
import com.familymemory.service.TimeCapsuleService;
import com.familymemory.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * 时光胶囊控制器
 */
@RestController
@RequiredArgsConstructor
public class TimeCapsuleController {

    private final TimeCapsuleService timeCapsuleService;

    /**
     * 获取胶囊列表
     */
    @GetMapping("/families/{familyId}/capsules")
    public Result<Map<String, Object>> getCapsules(
            @PathVariable Long familyId,
            @RequestParam(required = false, defaultValue = "all") String status) {
        return Result.success(timeCapsuleService.getCapsules(familyId, status));
    }

    /**
     * 创建胶囊
     */
    @PostMapping("/families/{familyId}/capsules")
    public Result<TimeCapsuleResp> createCapsule(
            @PathVariable Long familyId,
            @Valid @RequestBody CreateCapsuleReq req) {
        Long userId = UserContext.getUserId();
        return Result.success("创建成功", timeCapsuleService.createCapsule(familyId, req, userId));
    }

    /**
     * 获取胶囊详情
     */
    @GetMapping("/capsules/{capsuleId}")
    public Result<TimeCapsuleResp> getCapsule(@PathVariable Long capsuleId) {
        return Result.success(timeCapsuleService.getCapsuleById(capsuleId));
    }
}
