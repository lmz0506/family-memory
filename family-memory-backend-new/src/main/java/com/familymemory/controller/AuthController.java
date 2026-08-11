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

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.ok("注册成功", service.register(request));
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.ok("登录成功", service.login(request));
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        return ApiResult.ok(service.profile(UserContext.require()));
    }
}
