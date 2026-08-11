package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.FamilyRequest;
import com.familymemory.dto.InviteRequest;
import com.familymemory.dto.JoinFamilyRequest;
import com.familymemory.security.UserContext;
import com.familymemory.service.FamilyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/families")
public class FamilyController {
    private final FamilyService service;

    public FamilyController(FamilyService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.ok(service.list(UserContext.require()));
    }

    @PostMapping
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody FamilyRequest request) {
        return ApiResult.ok("家庭创建成功", service.create(request, UserContext.require()));
    }

    @GetMapping("/{familyId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long familyId) {
        return ApiResult.ok(service.detail(familyId, UserContext.require()));
    }

    @GetMapping("/{familyId}/members")
    public ApiResult<List<Map<String, Object>>> members(@PathVariable Long familyId) {
        return ApiResult.ok(service.members(familyId, UserContext.require()));
    }

    @PostMapping("/{familyId}/invite-codes")
    public ApiResult<Map<String, Object>> invite(@PathVariable Long familyId,
                                                 @RequestBody(required = false) InviteRequest request) {
        return ApiResult.ok(service.invite(familyId, request == null ? new InviteRequest() : request,
                UserContext.require()));
    }

    @PostMapping("/join")
    public ApiResult<Map<String, Object>> join(@Valid @RequestBody JoinFamilyRequest request) {
        return ApiResult.ok("欢迎加入家庭", service.join(request, UserContext.require()));
    }
}
