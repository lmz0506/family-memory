package com.familymemory.service;

import com.familymemory.dto.req.LoginReq;
import com.familymemory.dto.req.RegisterReq;
import com.familymemory.dto.req.UpdateProfileReq;
import com.familymemory.dto.resp.LoginResp;
import com.familymemory.dto.resp.UserResp;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    UserResp register(RegisterReq req);

    /**
     * 用户登录
     */
    LoginResp login(LoginReq req);

    /**
     * 获取当前用户信息
     */
    UserResp getCurrentUser(Long userId);

    /**
     * 更新用户资料
     */
    UserResp updateProfile(Long userId, UpdateProfileReq req);
}
