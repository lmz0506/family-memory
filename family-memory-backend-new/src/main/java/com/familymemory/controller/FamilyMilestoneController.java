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

@RestController
public class FamilyMilestoneController {
    private final FamilyMilestoneService service;

    public FamilyMilestoneController(FamilyMilestoneService service) {
        this.service = service;
    }

    @GetMapping("/families/{familyId}/milestones")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long familyId) {
        return ApiResult.ok(service.list(familyId, UserContext.require()));
    }

    @PostMapping("/families/{familyId}/milestones")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody FamilyMilestoneRequest request) {
        return ApiResult.ok("家庭大事记已添加", service.create(familyId, UserContext.require(), request));
    }

    @PutMapping("/milestones/{milestoneId}")
    public ApiResult<Map<String, Object>> update(@PathVariable Long milestoneId,
                                                 @Valid @RequestBody FamilyMilestoneRequest request) {
        return ApiResult.ok("家庭大事记已更新", service.update(milestoneId, UserContext.require(), request));
    }

    @DeleteMapping("/milestones/{milestoneId}")
    public ApiResult<Void> delete(@PathVariable Long milestoneId) {
        service.delete(milestoneId, UserContext.require());
        return ApiResult.ok("家庭大事记已删除", null);
    }
}
