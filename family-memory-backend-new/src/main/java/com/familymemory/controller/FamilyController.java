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

/**
 * 家庭空间、成员关系、邀请码及加入家庭接口。
 */
@RestController
@RequestMapping("/families")
public class FamilyController {
    /**
     * 家庭成员权限、家庭资料、邀请码及加入家庭业务。
     */
    private final FamilyService service;

    /**
     * 初始化 FamilyController 的依赖。
     * @param service 家庭成员权限、家庭资料、邀请码及加入家庭业务
     */
    public FamilyController(FamilyService service) {
        this.service = service;
    }

    /**
     * 查询当前用户的家庭列表。
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.ok(service.list(UserContext.require()));
    }

    /**
     * 创建家庭。
     * @param request 家庭空间创建请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping
    public ApiResult<Map<String, Object>> create(@Valid @RequestBody FamilyRequest request) {
        return ApiResult.ok("家庭创建成功", service.create(request, UserContext.require()));
    }

    /**
     * 查询指定家庭资料。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/{familyId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long familyId) {
        return ApiResult.ok(service.detail(familyId, UserContext.require()));
    }

    /**
     * 查询指定家庭成员。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/{familyId}/members")
    public ApiResult<List<Map<String, Object>>> members(@PathVariable Long familyId) {
        return ApiResult.ok(service.members(familyId, UserContext.require()));
    }

    /**
     * 生成家庭邀请码。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param request 家庭邀请码生成参数
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/{familyId}/invite-codes")
    public ApiResult<Map<String, Object>> invite(@PathVariable Long familyId,
                                                 @RequestBody(required = false) InviteRequest request) {
        return ApiResult.ok(service.invite(familyId, request == null ? new InviteRequest() : request,
                UserContext.require()));
    }

    /**
     * 使用有效邀请码加入家庭。
     * @param request 使用邀请码加入家庭的请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/join")
    public ApiResult<Map<String, Object>> join(@Valid @RequestBody JoinFamilyRequest request) {
        return ApiResult.ok("欢迎加入家庭", service.join(request, UserContext.require()));
    }
}
