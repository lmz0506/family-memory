package com.familymemory.service;

import com.familymemory.common.PageResult;
import com.familymemory.dto.req.CreateMemoryReq;
import com.familymemory.dto.req.MemoryQueryReq;
import com.familymemory.dto.resp.MemoryResp;

import java.util.List;

/**
 * 记忆服务接口
 */
public interface MemoryService {

    /**
     * 获取记忆列表
     */
    PageResult<MemoryResp> getMemories(Long familyId, MemoryQueryReq req, Long userId);

    /**
     * 获取记忆详情
     */
    MemoryResp getMemoryById(Long memoryId, Long userId);

    /**
     * 创建记忆
     */
    MemoryResp createMemory(Long familyId, CreateMemoryReq req, Long userId);

    /**
     * 点赞记忆
     */
    MemoryResp likeMemory(Long memoryId, Long userId);

    /**
     * 取消点赞
     */
    MemoryResp unlikeMemory(Long memoryId, Long userId);

    /**
     * 获取那年今日记忆
     */
    List<MemoryResp> getTodayInHistory(Long familyId, Long userId);

    /**
     * 获取可用年份列表
     */
    List<String> getYears(Long familyId);

    /**
     * 删除记忆
     */
    void deleteMemory(Long memoryId, Long userId);
}
