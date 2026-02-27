package com.familymemory.controller;

import com.familymemory.common.PageResult;
import com.familymemory.common.Result;
import com.familymemory.dto.req.CreateMemoryReq;
import com.familymemory.dto.req.MemoryQueryReq;
import com.familymemory.dto.resp.MemoryResp;
import com.familymemory.service.MemoryService;
import com.familymemory.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 记忆控制器
 */
@RestController
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    /**
     * 获取记忆列表
     */
    @GetMapping("/families/{familyId}/memories")
    public Result<PageResult<MemoryResp>> getMemories(
            @PathVariable Long familyId,
            MemoryQueryReq req) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.getMemories(familyId, req, userId));
    }

    /**
     * 获取记忆详情
     */
    @GetMapping("/memories/{memoryId}")
    public Result<MemoryResp> getMemory(@PathVariable Long memoryId) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.getMemoryById(memoryId, userId));
    }

    /**
     * 创建记忆
     */
    @PostMapping("/families/{familyId}/memories")
    public Result<MemoryResp> createMemory(
            @PathVariable Long familyId,
            @Valid @RequestBody CreateMemoryReq req) {
        Long userId = UserContext.getUserId();
        return Result.success("创建成功", memoryService.createMemory(familyId, req, userId));
    }

    /**
     * 点赞记忆
     */
    @PostMapping("/memories/{memoryId}/like")
    public Result<MemoryResp> likeMemory(@PathVariable Long memoryId) {
        Long userId = UserContext.getUserId();
        return Result.success("点赞成功", memoryService.likeMemory(memoryId, userId));
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/memories/{memoryId}/like")
    public Result<MemoryResp> unlikeMemory(@PathVariable Long memoryId) {
        Long userId = UserContext.getUserId();
        return Result.success("取消点赞", memoryService.unlikeMemory(memoryId, userId));
    }

    /**
     * 获取那年今日记忆
     */
    @GetMapping("/families/{familyId}/memories/today-in-history")
    public Result<List<MemoryResp>> getTodayInHistory(@PathVariable Long familyId) {
        Long userId = UserContext.getUserId();
        return Result.success(memoryService.getTodayInHistory(familyId, userId));
    }

    /**
     * 获取可用年份列表
     */
    @GetMapping("/families/{familyId}/memories/years")
    public Result<List<String>> getYears(@PathVariable Long familyId) {
        return Result.success(memoryService.getYears(familyId));
    }

    /**
     * 删除记忆
     */
    @DeleteMapping("/memories/{memoryId}")
    public Result<Void> deleteMemory(@PathVariable Long memoryId) {
        Long userId = UserContext.getUserId();
        memoryService.deleteMemory(memoryId, userId);
        return Result.success("删除成功", null);
    }
}
