package com.example.itodo.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.itodo.auth.AuthService;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.user.dto.ChangePasswordRequest;
import com.example.itodo.user.dto.UpdateUserProfileRequest;
import com.example.itodo.user.dto.UserProfileResponse;
import com.example.itodo.user.entity.User;
import com.example.itodo.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final UserDtoMapper userDtoMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserService(UserMapper userMapper, UserDtoMapper userDtoMapper, PasswordEncoder passwordEncoder, AuthService authService) {
        this.userMapper = userMapper;
        this.userDtoMapper = userDtoMapper;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public UserProfileResponse getMe(UUID userId) {
        return userDtoMapper.toProfileResponse(requireActiveUser(userId));
    }

    @Transactional
    public UserProfileResponse updateMe(UUID userId, UpdateUserProfileRequest request) {
        User user = requireActiveUser(userId);
        boolean changed = false;
        if (request.displayName() != null) {
            user.setDisplayName(normalize(request.displayName()));
            changed = true;
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(normalize(request.avatarUrl()));
            changed = true;
        }
        if (changed) {
            user.setUpdatedAt(Instant.now());
            userMapper.updateById(user);
        }
        return userDtoMapper.toProfileResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = requireActiveUser(userId);
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Password login is not enabled");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Current password is incorrect");
        }
        if (Objects.equals(request.currentPassword(), request.newPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "New password must be different from current password");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .isNull(User::getDeletedAt)
                .set(User::getPasswordHash, passwordEncoder.encode(request.newPassword()))
                .set(User::getUpdatedAt, Instant.now()));
        authService.revokeAll(userId);
    }

    @Transactional
    public void deleteMe(UUID userId) {
        requireActiveUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .isNull(User::getDeletedAt)
                .set(User::getStatus, UserStatus.DELETED)
                .set(User::getDeletedAt, Instant.now())
                .set(User::getUpdatedAt, Instant.now()));
        authService.revokeAll(userId);
    }

    private User requireActiveUser(UUID userId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, userId)
                .eq(User::getStatus, UserStatus.ACTIVE)
                .isNull(User::getDeletedAt));
        if (user == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "User not found");
        }
        return user;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
