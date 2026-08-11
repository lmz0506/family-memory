package com.familymemory.controller;

import com.familymemory.common.ApiResult;
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

@RestController
public class MemoryController {
    private final MemoryService service;

    public MemoryController(MemoryService service) {
        this.service = service;
    }

    @GetMapping("/families/{familyId}/memories")
    public ApiResult<Map<String, Object>> list(@PathVariable Long familyId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size, @RequestParam(required =
                    false) Integer year, @RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.list(familyId, UserContext.require(), page, size, year, keyword));
    }

    @PostMapping("/families/{familyId}/memories")
    public ApiResult<Map<String, Object>> create(@PathVariable Long familyId,
                                                 @Valid @RequestBody MemoryRequest request) {
        return ApiResult.ok("记忆已珍藏", service.create(familyId, UserContext.require(), request));
    }

    @GetMapping("/families/{familyId}/memories/today-in-history")
    public ApiResult<List<Map<String, Object>>> today(@PathVariable Long familyId) {
        return ApiResult.ok(service.today(familyId, UserContext.require()));
    }

    @GetMapping("/families/{familyId}/memories/years")
    public ApiResult<List<Integer>> years(@PathVariable Long familyId) {
        return ApiResult.ok(service.years(familyId, UserContext.require()));
    }

    @GetMapping("/families/{familyId}/memories/timeline")
    public ApiResult<List<Map<String, Object>>> timelineOverview(@PathVariable Long familyId) {
        return ApiResult.ok(service.timelineOverview(familyId, UserContext.require()));
    }

    @GetMapping("/families/{familyId}/memories/timeline/{year}/{month}")
    public ApiResult<List<Map<String, Object>>> timelineMonth(@PathVariable Long familyId,
                                                              @PathVariable int year,
                                                              @PathVariable int month) {
        return ApiResult.ok(service.timelineMonth(familyId, UserContext.require(), year, month));
    }

    @GetMapping("/families/{familyId}/memories/timeline/stars")
    public ApiResult<List<Map<String, Object>>> timelineStars(@PathVariable Long familyId) {
        return ApiResult.ok(service.timelineStars(familyId, UserContext.require()));
    }

    @GetMapping("/memories/{memoryId}")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long memoryId) {
        return ApiResult.ok(service.detail(memoryId, UserContext.require()));
    }

    @PostMapping("/memories/{memoryId}/like")
    public ApiResult<Map<String, Object>> like(@PathVariable Long memoryId) {
        return ApiResult.ok(service.toggleLike(memoryId, UserContext.require()));
    }

    @PostMapping("/memories/{memoryId}/comments")
    public ApiResult<Map<String, Object>> comment(@PathVariable Long memoryId,
                                                  @Valid @RequestBody CommentRequest request) {
        return ApiResult.ok("回应已发送", service.addComment(memoryId, UserContext.require(), request));
    }

    @DeleteMapping("/memories/{memoryId}")
    public ApiResult<Void> delete(@PathVariable Long memoryId) {
        service.delete(memoryId, UserContext.require());
        return ApiResult.ok("记忆已删除", null);
    }
}
