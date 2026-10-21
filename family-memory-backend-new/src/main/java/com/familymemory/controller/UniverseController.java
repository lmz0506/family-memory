package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.UniverseOverviewResponse;
import com.familymemory.dto.UniversePageResponse;
import com.familymemory.dto.UniverseBatchResponse;
import com.familymemory.security.UserContext;
import com.familymemory.service.UniverseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

/** 星海统一索引查询接口，登录用户必须属于目标家庭。 */
@RestController
public class UniverseController {
    /** 星海只读查询服务。 */
    private final UniverseService service;

    /**
     * 初始化星海查询控制器。
     * @param service 星海只读查询服务
     */
    public UniverseController(UniverseService service) { this.service = service; }

    /**
     * 获取轻量年月日计数概要。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @return 不含逐条内容的层级统计
     */
    @GetMapping("/families/{familyId}/universe/overview")
    public ApiResult<UniverseOverviewResponse> overview(@PathVariable Long familyId) {
        return ApiResult.ok(service.overview(familyId, UserContext.require()));
    }

    /**
     * 后台按年份游标分批预加载记忆与珍藏星点。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param year 归档年份，范围1至9998
     * @param afterId 排他索引游标，来源表：fm_universe_index.id，首次为0
     * @param size 批量大小，默认120，限制为1至200
     * @return 轻量星点批次及下一批游标
     */
    @GetMapping("/families/{familyId}/universe/year")
    public ApiResult<UniverseBatchResponse> year(@PathVariable Long familyId, @RequestParam int year,
        @RequestParam(defaultValue = "0") long afterId, @RequestParam(defaultValue = "120") int size) {
        return ApiResult.ok(service.year(familyId, UserContext.require(), year, afterId, size));
    }

    /**
     * 分页获取某一天的记忆和珍藏索引。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param date 归档日期，格式yyyy-MM-dd
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；省略表示全部
     * @param page 页码，从1开始
     * @param size 页大小，实际限制为1至50
     * @return 轻量内容分页
     */
    @GetMapping("/families/{familyId}/universe/day")
    public ApiResult<UniversePageResponse> day(@PathVariable Long familyId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String sourceType,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResult.ok(service.day(familyId, UserContext.require(), date, sourceType, page, size));
    }

    /**
     * 统一搜索索引标题，不拉取全部业务列表再由浏览器筛选。
     * @param familyId 家庭ID，来源表：fm_family.id
     * @param keyword 标题关键词，最多120字
     * @param sourceType 来源类型，MEMORY 记忆，CAPSULE 珍藏；省略表示全部
     * @param page 页码，从1开始
     * @param size 页大小，实际限制为1至50
     * @return 带日期路径的匹配内容分页
     */
    @GetMapping("/families/{familyId}/universe/search")
    public ApiResult<UniversePageResponse> search(@PathVariable Long familyId,
        @RequestParam(defaultValue = "") String keyword, @RequestParam(required = false) String sourceType,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "24") int size) {
        return ApiResult.ok(service.search(familyId, UserContext.require(), keyword, sourceType, page, size));
    }
}
