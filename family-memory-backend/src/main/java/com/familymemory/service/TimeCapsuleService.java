package com.familymemory.service;

import com.familymemory.dto.req.CreateCapsuleReq;
import com.familymemory.dto.resp.TimeCapsuleResp;

import java.util.List;
import java.util.Map;

/**
 * 时光胶囊服务接口
 */
public interface TimeCapsuleService {

    /**
     * 获取胶囊列表
     */
    Map<String, Object> getCapsules(Long familyId, String status);

    /**
     * 创建胶囊
     */
    TimeCapsuleResp createCapsule(Long familyId, CreateCapsuleReq req, Long userId);

    /**
     * 获取胶囊详情
     */
    TimeCapsuleResp getCapsuleById(Long capsuleId);
}
