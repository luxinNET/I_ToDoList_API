package com.example.itodo.infra.ratelimit;

import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final RedissonClient redissonClient;
    private final RateLimitProperties properties;

    public RateLimiterService(RedissonClient redissonClient, RateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    public void checkLogin(String key) {
        check("login:" + key, properties.login());
    }

    public void checkRegister(String key) {
        check("register:" + key, properties.register());
    }

    public void checkRefresh(String key) {
        check("refresh:" + key, properties.refresh());
    }

    private void check(String key, RateLimitProperties.Rule rule) {
        RRateLimiter limiter = redissonClient.getRateLimiter("rate-limit:" + key);
        limiter.trySetRate(RateType.OVERALL, rule.permits(), rule.intervalSeconds(), RateIntervalUnit.SECONDS);
        if (!limiter.tryAcquire()) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "Too many requests");
        }
    }
}
