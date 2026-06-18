package com.example.itodo.auth;

import com.example.itodo.auth.dto.AuthResponse;
import com.example.itodo.auth.dto.LoginRequest;
import com.example.itodo.auth.dto.RefreshTokenRequest;
import com.example.itodo.auth.dto.RegisterRequest;
import com.example.itodo.auth.dto.WechatMiniProgramLoginRequest;
import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.user.UserService;
import com.example.itodo.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @SecurityRequirements
    @Operation(summary = "邮箱或手机号注册")
    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.register(request, ClientContext.from(servletRequest, null, null)));
    }

    @SecurityRequirements
    @Operation(summary = "邮箱/手机号 + 密码登录")
    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.login(request, ClientContext.from(servletRequest, request.clientType(), request.deviceId())));
    }

    @SecurityRequirements
    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.refresh(request, ClientContext.from(servletRequest, request.clientType(), request.deviceId())));
    }

    @SecurityRequirements
    @Operation(summary = "微信小程序 code 登录")
    @PostMapping("/wechat-mini-program/login")
    ApiResponse<AuthResponse> wechatMiniProgramLogin(@Valid @RequestBody WechatMiniProgramLoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(authService.wechatMiniProgramLogin(request, ClientContext.from(servletRequest, request.clientType(), request.deviceId())));
    }

    @Operation(summary = "退出当前设备", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUser currentUser) {
        authService.logout(currentUser);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "退出所有设备", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout-all")
    ApiResponse<Void> logoutAll(@AuthenticationPrincipal CurrentUser currentUser) {
        authService.logoutAll(currentUser);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "当前认证用户", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(userService.getMe(currentUser.id()));
    }
}
