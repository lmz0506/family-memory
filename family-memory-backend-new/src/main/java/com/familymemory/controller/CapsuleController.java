package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.TitleUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

/**
 * 珍藏接口，封存内容按开启日期控制读取，管理操作仅允许创建者。
 */
@RestController
public class CapsuleController {
    /**
     * 珍藏封存与读取业务，索引只使用封存日期，正文受开启条件约束。
     */
    private final CapsuleService service;

    /**
     * 初始化 CapsuleController 的依赖。
     * @param service 珍藏封存与读取业务，索引只使用封存日期，正文受开启条件约束
     */
    public CapsuleController(CapsuleService service) {
        this.service = service;
    }

    /**
     * 查询家庭珍藏元数据。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/capsules")
    public ApiResult<List<Map<String, Object>>> list(@PathVariable Long familyId) {
        return ApiResult.ok(service.list(familyId, UserContext.require()));
    }

    /**
     * 封存一份写给未来的珍藏。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param request 珍藏封存请求，开启日期决定正文可读时间
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/families/{familyId}/capsules")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody CapsuleRequest request) {
        return ApiResult.ok("时光胶囊已封存", service.create(familyId, UserContext.require(), request));
    }

    /**
     * 按原业务开启规则查看珍藏详情。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/capsules/{capsuleId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long capsuleId) {
        return ApiResult.ok(service.detail(capsuleId, UserContext.require()));
    }

    /**
     * 修改本人珍藏标题，同事务更新星海索引。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @param request 新标题，不包含日期字段
     * @return 修改结果
     */
    @PutMapping("/capsules/{capsuleId}/title")
    public ApiResult<Void> updateTitle(@PathVariable Long capsuleId, @Valid @RequestBody TitleUpdateRequest request) {
        service.updateTitle(capsuleId, UserContext.require(), request.getTitle());
        return ApiResult.ok("标题已更新", null);
    }

    /**
     * 删除本人珍藏并同步删除星海索引。
     * @param capsuleId 珍藏ID，来源表：fm_time_capsule.id
     * @return 删除结果
     */
    @DeleteMapping("/capsules/{capsuleId}")
    public ApiResult<Void> delete(@PathVariable Long capsuleId) {
        service.delete(capsuleId, UserContext.require());
        return ApiResult.ok("珍藏已删除", null);
    }
}
