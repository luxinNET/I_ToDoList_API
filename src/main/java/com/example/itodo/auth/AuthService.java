package com.example.itodo.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.itodo.auth.dto.AuthResponse;
import com.example.itodo.auth.dto.LoginRequest;
import com.example.itodo.auth.dto.RefreshTokenRequest;
import com.example.itodo.auth.dto.RegisterRequest;
import com.example.itodo.auth.dto.WechatMiniProgramLoginRequest;
import com.example.itodo.auth.entity.RefreshToken;
import com.example.itodo.auth.mapper.RefreshTokenMapper;
import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.example.itodo.infra.ratelimit.RateLimiterService;
import com.example.itodo.security.CurrentUser;
import com.example.itodo.security.JwtProperties;
import com.example.itodo.security.JwtService;
import com.example.itodo.user.UserStatus;
import com.example.itodo.user.entity.User;
import com.example.itodo.user.entity.UserIdentity;
import com.example.itodo.user.mapper.UserIdentityMapper;
import com.example.itodo.user.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {

    private static final String GENERIC_LOGIN_FAILURE = "Invalid account or password";
    private static final String GENERIC_REFRESH_FAILURE = "Invalid refresh token";

    private final UserMapper userMapper;
    private final UserIdentityMapper userIdentityMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RateLimiterService rateLimiterService;
    private final WechatMiniProgramClient wechatMiniProgramClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserMapper userMapper,
                       UserIdentityMapper userIdentityMapper,
                       RefreshTokenMapper refreshTokenMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       RateLimiterService rateLimiterService,
                       WechatMiniProgramClient wechatMiniProgramClient) {
        this.userMapper = userMapper;
        this.userIdentityMapper = userIdentityMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.rateLimiterService = rateLimiterService;
        this.wechatMiniProgramClient = wechatMiniProgramClient;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, ClientContext clientContext) {
        String email = normalizeEmail(request.email());
        String phone = normalize(request.phone());
        rateLimiterService.checkRegister(firstText(email, phone, clientContext.ipAddress(), "anonymous"));

        if (!StringUtils.hasText(email) && !StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Email or phone is required");
        }
        if (StringUtils.hasText(email) && findActiveByEmail(email) != null) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Email is already registered");
        }
        if (StringUtils.hasText(phone) && findActiveByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.RESOURCE_CONFLICT, "Phone is already registered");
        }

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPhone(phone);
        user.setDisplayName(normalize(request.displayName()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        return issueTokenPair(user, clientContext);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, ClientContext clientContext) {
        String account = normalize(request.account());
        rateLimiterService.checkLogin(firstText(account, clientContext.ipAddress(), "anonymous"));

        User user = findActiveByAccount(account);
        if (user == null || !StringUtils.hasText(user.getPasswordHash()) || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, GENERIC_LOGIN_FAILURE);
        }
        return issueTokenPair(user, clientContext);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, ClientContext clientContext) {
        String tokenHash = hashToken(request.refreshToken());
        rateLimiterService.checkRefresh(tokenHash.substring(0, 16));

        RefreshToken refreshToken = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash));
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, GENERIC_REFRESH_FAILURE);
        }
        if (refreshToken.getRevokedAt() != null) {
            revokeAll(refreshToken.getUserId());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, GENERIC_REFRESH_FAILURE);
        }
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, GENERIC_REFRESH_FAILURE);
        }
        if (StringUtils.hasText(clientContext.deviceId()) && StringUtils.hasText(refreshToken.getDeviceId())
                && !Objects.equals(clientContext.deviceId(), refreshToken.getDeviceId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, GENERIC_REFRESH_FAILURE);
        }

        User user = userMapper.selectById(refreshToken.getUserId());
        if (!isActive(user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "User is not active");
        }

        Instant now = Instant.now();
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getId, refreshToken.getId())
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, now));

        ClientContext persistedContext = new ClientContext(
                firstText(clientContext.clientType(), refreshToken.getClientType()),
                firstText(clientContext.deviceId(), refreshToken.getDeviceId()),
                clientContext.ipAddress(),
                clientContext.userAgent());
        return issueTokenPair(user, persistedContext);
    }

    @Transactional
    public AuthResponse wechatMiniProgramLogin(WechatMiniProgramLoginRequest request, ClientContext clientContext) {
        WechatMiniProgramClient.WechatSession session = wechatMiniProgramClient.exchangeCode(request.code());
        UserIdentity identity = userIdentityMapper.selectOne(new LambdaQueryWrapper<UserIdentity>()
                .eq(UserIdentity::getProvider, UserIdentity.PROVIDER_WECHAT_MINI_PROGRAM)
                .eq(UserIdentity::getProviderSubject, session.openId()));

        User user;
        if (identity == null) {
            user = createWechatUser(session);
        } else {
            user = userMapper.selectById(identity.getUserId());
            if (!isActive(user)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "User is not active");
            }
        }
        return issueTokenPair(user, clientContext);
    }

    @Transactional
    public void logout(CurrentUser currentUser) {
        if (StringUtils.hasText(currentUser.deviceId())) {
            revokeDevice(currentUser.id(), currentUser.deviceId());
        } else {
            revokeAll(currentUser.id());
        }
    }

    @Transactional
    public void logoutAll(CurrentUser currentUser) {
        revokeAll(currentUser.id());
    }

    @Transactional
    public void revokeAll(UUID userId) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, Instant.now()));
    }

    private void revokeDevice(UUID userId, String deviceId) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .eq(RefreshToken::getDeviceId, deviceId)
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, Instant.now()));
    }

    private User createWechatUser(WechatMiniProgramClient.WechatSession session) {
        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("wx_" + session.openId());
        user.setDisplayName("微信用户");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUserId(user.getId());
        identity.setProvider(UserIdentity.PROVIDER_WECHAT_MINI_PROGRAM);
        identity.setProviderSubject(session.openId());
        identity.setOpenId(session.openId());
        identity.setUnionId(session.unionId());
        identity.setCreatedAt(now);
        userIdentityMapper.insert(identity);
        return user;
    }

    private AuthResponse issueTokenPair(User user, ClientContext clientContext) {
        CurrentUser currentUser = new CurrentUser(user.getId(), principalName(user), clientContext.clientType(), clientContext.deviceId());
        JwtService.IssuedAccessToken accessToken = jwtService.issue(currentUser);
        String rawRefreshToken = generateRefreshToken();
        Instant refreshTokenExpiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl());
        saveRefreshToken(user.getId(), rawRefreshToken, refreshTokenExpiresAt, clientContext);
        return new AuthResponse(user.getId(), accessToken.token(), rawRefreshToken, accessToken.expiresAt(), refreshTokenExpiresAt);
    }

    private void saveRefreshToken(UUID userId, String rawRefreshToken, Instant expiresAt, ClientContext clientContext) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashToken(rawRefreshToken));
        refreshToken.setClientType(clientContext.clientType());
        refreshToken.setDeviceId(clientContext.deviceId());
        refreshToken.setUserAgent(clientContext.userAgent());
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setCreatedAt(Instant.now());
        refreshTokenMapper.insert(refreshToken);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private User findActiveByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userMapper.selectOne(activeUsers().eq(User::getEmail, email));
    }

    private User findActiveByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return userMapper.selectOne(activeUsers().eq(User::getPhone, phone));
    }

    private User findActiveByAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        return userMapper.selectOne(activeUsers()
                .and(wrapper -> wrapper.eq(User::getEmail, normalizeEmail(account))
                        .or()
                        .eq(User::getPhone, account)
                        .or()
                        .eq(User::getUsername, account)));
    }

    private LambdaQueryWrapper<User> activeUsers() {
        return new LambdaQueryWrapper<User>()
                .eq(User::getStatus, UserStatus.ACTIVE)
                .isNull(User::getDeletedAt);
    }

    private boolean isActive(User user) {
        return user != null && UserStatus.ACTIVE.equals(user.getStatus()) && user.getDeletedAt() == null;
    }

    private String principalName(User user) {
        return firstText(user.getUsername(), user.getDisplayName(), user.getEmail(), user.getPhone());
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
