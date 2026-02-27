package com.familymemory.controller;

import com.familymemory.common.Result;
import com.familymemory.dto.req.LoginReq;
import com.familymemory.dto.req.RegisterReq;
import com.familymemory.dto.req.UpdateProfileReq;
import com.familymemory.dto.resp.LoginResp;
import com.familymemory.dto.resp.UserResp;
import com.familymemory.service.AuthService;
import com.familymemory.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserResp> register(@Valid @RequestBody RegisterReq req) {
        return Result.success("注册成功", authService.register(req));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.success("登录成功", authService.login(req));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserResp> getCurrentUser() {
        Long userId = UserContext.getUserId();
        return Result.success(authService.getCurrentUser(userId));
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<UserResp> updateProfile(@RequestBody UpdateProfileReq req) {
        Long userId = UserContext.getUserId();
        return Result.success("保存成功", authService.updateProfile(userId, req));
    }
}
