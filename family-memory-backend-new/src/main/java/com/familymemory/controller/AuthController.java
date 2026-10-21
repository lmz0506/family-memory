package com.familymemory.controller;

import com.familymemory.common.ApiResult;
import com.familymemory.dto.LoginRequest;
import com.familymemory.dto.RegisterRequest;
import com.familymemory.security.UserContext;
import com.familymemory.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

/**
 * 用户认证接口：注册、登录和当前登录用户资料。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {
    /**
     * 认证业务，校验密码并签发JWT，不向客户端返回密码摘要。
     */
    private final AuthService service;

    /**
     * 初始化 AuthController 的依赖。
     * @param service 认证业务，校验密码并签发JWT，不向客户端返回密码摘要
     */
    public AuthController(AuthService service) {
        this.service = service;
    }

    /**
     * 注册新账号。
     * @param request 新用户注册请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok("注册成功", service.register(request));
    }

    /**
     * 账号密码登录。
     * @param request 账号密码登录请求
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok("登录成功", service.login(request));
    }

    /**
     * 查询当前登录用户资料。
     * @return 业务响应字段，逐字段中文说明见响应组装处
     */
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(service.profile(UserContext.require()));
    }
}
