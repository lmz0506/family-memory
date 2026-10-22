package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.TitleUpdateRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.familymemory.dto.CommentRequest;
import com.familymemory.dto.MemoryRequest;
import com.familymemory.security.UserContext;
import com.familymemory.service.MemoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 家庭记忆接口，读写业务执行家庭成员及发布者权限校验。
 */
@RestController
public class MemoryController {
    /**
     * 家庭记忆业务，发布、改标题和删除同事务维护星海索引。
     */
    private final MemoryService service;

    /**
     * 初始化 MemoryController 的依赖。
     * @param service 家庭记忆业务，发布、改标题和删除同事务维护星海索引
     */
    public MemoryController(MemoryService service) {
        this.service = service;
    }

    /**
     * 分页查询传统家庭记忆列表。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param page 页码，从1开始
     * @param size 请求分页大小，服务端限制最大值
     * @param year 目标年份
     * @param keyword 标题模糊搜索关键词，空值表示不限制标题
     * @param authorId 作者用户ID，来源表：fm_user.id；空值表示所有作者
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/memories")
    public ApiResult<Map<String, Object>> list(@PathVariable Long familyId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size, @RequestParam(required =
                    false) Integer year, @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long authorId) {
        return ApiResult.ok(service.list(familyId, UserContext.require(), page, size, year, keyword, authorId));
    }

    /**
     * 发布家庭记忆。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param request 记忆发布请求，包含发生日期、正文及媒体资源地址
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/families/{familyId}/memories")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody MemoryRequest request) {
        return ApiResult.ok("记忆已珍藏", service.create(familyId, UserContext.require(), request));
    }

    /**
     * 查询历史上的今天。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/memories/today-in-history")
    public ApiResult<List<Map<String, Object>>> today(@PathVariable Long familyId) {
        return ApiResult.ok(service.today(familyId, UserContext.require()));
    }

    /**
     * 查询传统记忆年份列表。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 统一接口响应及业务结果
     */
    @GetMapping("/families/{familyId}/memories/years")
    public ApiResult<List<Integer>> years(@PathVariable Long familyId) {
        return ApiResult.ok(service.years(familyId, UserContext.require()));
    }

    /**
     * 查询旧时间轴概要，新版星海使用universe接口。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/memories/timeline")
    public ApiResult<List<Map<String, Object>>> timelineOverview(@PathVariable Long familyId) {
        return ApiResult.ok(service.timelineOverview(familyId, UserContext.require()));
    }

    /**
     * 查询旧时间轴某月列表。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param year 目标年份
     * @param month 月份，1至12
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/memories/timeline/{year}/{month}")
    public ApiResult<List<Map<String, Object>>> timelineMonth(@PathVariable Long familyId,
                                                              @PathVariable int year,
                                                              @PathVariable int month) {
        return ApiResult.ok(service.timelineMonth(familyId, UserContext.require(), year, month));
    }

    /**
     * 兼容旧版逐条星点查询，新版星海不再调用。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/families/{familyId}/memories/timeline/stars")
    public ApiResult<List<Map<String, Object>>> timelineStars(@PathVariable Long familyId) {
        return ApiResult.ok(service.timelineStars(familyId, UserContext.require()));
    }

    /**
     * 查看家庭记忆详情。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/memories/{memoryId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long memoryId) {
        return ApiResult.ok(service.detail(memoryId, UserContext.require()));
    }

    /**
     * 切换记忆点赞状态。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/memories/{memoryId}/like")
    public ApiResult<Map<String, Object>> like(@PathVariable Long memoryId) {
        return ApiResult.ok(service.toggleLike(memoryId, UserContext.require()));
    }

    /**
     * 发布记忆评论。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param request 记忆评论发布请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/memories/{memoryId}/comments")
    public ApiResult<Map<String, Object>> comment(@PathVariable Long memoryId,
                                                  @Valid @RequestBody CommentRequest request) {
        return ApiResult.ok("回应已发送", service.addComment(memoryId, UserContext.require(), request));
    }

    /**
     * 删除本人发布的记忆并同步星海索引。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @return 统一接口响应及业务结果
     */
    @DeleteMapping("/memories/{memoryId}")
    public ApiResult<Void> delete(@PathVariable Long memoryId) {
        service.delete(memoryId, UserContext.require());
        return ApiResult.ok("记忆已删除", null);
    }

    /**
     * 修改本人记忆标题，同事务更新星海索引。
     * @param memoryId 记忆ID，来源表：fm_memory.id
     * @param request 新标题，不包含日期字段
     * @return 修改结果
     */
    @PutMapping("/memories/{memoryId}/title")
    public ApiResult<Void> updateTitle(@PathVariable Long memoryId, @Valid @RequestBody TitleUpdateRequest request) {
        service.updateTitle(memoryId, UserContext.require(), request.getTitle());
        return ApiResult.ok("标题已更新", null);
    }
}
