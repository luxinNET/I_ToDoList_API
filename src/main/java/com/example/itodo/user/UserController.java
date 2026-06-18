package com.example.itodo.user;

import com.example.itodo.common.api.ApiResponse;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.user.dto.ChangePasswordRequest;
import com.example.itodo.user.dto.UpdateUserProfileRequest;
import com.example.itodo.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取我的资料")
    @GetMapping("/me")
    ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(userService.getMe(currentUser.id()));
    }

    @Operation(summary = "更新我的资料")
    @PatchMapping("/me")
    ApiResponse<UserProfileResponse> updateMe(@AuthenticationPrincipal CurrentUser currentUser,
                                               @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.ok(userService.updateMe(currentUser.id(), request));
    }

    @Operation(summary = "删除我的账号")
    @DeleteMapping("/me")
    ApiResponse<Void> deleteMe(@AuthenticationPrincipal CurrentUser currentUser) {
        userService.deleteMe(currentUser.id());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "修改我的密码")
    @PatchMapping("/me/password")
    ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                      @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUser.id(), request);
        return ApiResponse.ok(null);
    }
}
