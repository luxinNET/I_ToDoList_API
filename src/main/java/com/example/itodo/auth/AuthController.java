package com.example.itodo.auth;

import com.example.itodo.auth.dto.AuthResponse;
import com.example.itodo.auth.dto.LoginRequest;
import com.example.itodo.auth.dto.RefreshTokenRequest;
import com.example.itodo.auth.dto.RegisterRequest;
import com.example.itodo.auth.dto.WechatMiniProgramLoginRequest;
import com.example.itodo.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Operation(summary = "邮箱或手机号注册")
    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(AuthResponse.placeholder("register"));
    }

    @Operation(summary = "邮箱/手机号 + 密码登录")
    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(AuthResponse.placeholder("login"));
    }

    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(AuthResponse.placeholder("refresh"));
    }

    @Operation(summary = "微信小程序 code 登录")
    @PostMapping("/wechat-mini-program/login")
    ApiResponse<AuthResponse> wechatMiniProgramLogin(@Valid @RequestBody WechatMiniProgramLoginRequest request) {
        return ApiResponse.ok(AuthResponse.placeholder("wechat-mini-program"));
    }

    @Operation(summary = "退出当前设备")
    @PostMapping("/logout")
    ApiResponse<Void> logout() {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "退出所有设备")
    @PostMapping("/logout-all")
    ApiResponse<Void> logoutAll() {
        return ApiResponse.ok(null);
    }

    @Operation(summary = "当前认证用户")
    @GetMapping("/me")
    ApiResponse<String> me() {
        return ApiResponse.ok("JWT security implementation pending");
    }
}
