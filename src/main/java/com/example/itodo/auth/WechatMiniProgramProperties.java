package com.example.itodo.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wechat.mini-program")
public record WechatMiniProgramProperties(
        String appId,
        String secret
) {
}
