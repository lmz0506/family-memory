package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.CapsuleRequest;
import com.familymemory.security.UserContext;
import com.familymemory.service.CapsuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
public class CapsuleController {
    private final CapsuleService service;

    public CapsuleController(CapsuleService service) {
        this.service = service;
    }

    @GetMapping("/families/{familyId}/capsules")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long familyId) {
        return ApiResult.ok(service.list(familyId, UserContext.require()));
    }

    @PostMapping("/families/{familyId}/capsules")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody CapsuleRequest request) {
        return ApiResult.ok("时光胶囊已封存", service.create(familyId, UserContext.require(), request));
    }

    @GetMapping("/capsules/{capsuleId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long capsuleId) {
        return ApiResult.ok(service.detail(capsuleId, UserContext.require()));
    }
}
