package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.FamilyMilestoneRequest;
import com.familymemory.security.UserContext;
import com.familymemory.service.FamilyMilestoneService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 家庭大事记提醒接口，支持创建者管理及可见范围控制。
 */
@RestController
public class FamilyMilestoneController {
    /**
     * 家庭提醒业务，处理可见范围、指定成员及年度重复日期。
     */
    private final FamilyMilestoneService service;

    /**
     * 初始化 FamilyMilestoneController 的依赖。
     * @param service 家庭提醒业务，处理可见范围、指定成员及年度重复日期
     */
    public FamilyMilestoneController(FamilyMilestoneService service) {
        this.service = service;
    }

    /**
     * 查询当前用户可见的家庭提醒。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/milestones")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long familyId) {
        return ApiResult.ok(service.list(familyId, UserContext.require()));
    }

    /**
     * 创建家庭提醒。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param request 家庭提醒创建或修改请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/families/{familyId}/milestones")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody FamilyMilestoneRequest request) {
        return ApiResult.ok("家庭大事记已添加", service.create(familyId, UserContext.require(), request));
    }

    /**
     * 修改本人家庭提醒。
     * @param milestoneId 大事记ID，来源表：fm_family_milestone.id
     * @param request 家庭提醒创建或修改请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PutMapping("/milestones/{milestoneId}")
    public ApiResult<Map<String, Object>> update(@PathVariable Long milestoneId,
                                                 @Valid @RequestBody FamilyMilestoneRequest request) {
        return ApiResult.ok("家庭大事记已更新", service.update(milestoneId, UserContext.require(), request));
    }

    /**
     * 删除本人家庭提醒。
     * @param milestoneId 大事记ID，来源表：fm_family_milestone.id
     * @return 统一接口响应及业务结果
     */
    @DeleteMapping("/milestones/{milestoneId}")
    public ApiResult<Void> delete(@PathVariable Long milestoneId) {
        service.delete(milestoneId, UserContext.require());
        return ApiResult.ok("家庭大事记已删除", null);
    }
}
