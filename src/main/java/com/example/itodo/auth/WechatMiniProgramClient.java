package com.example.itodo.auth;

public interface WechatMiniProgramClient {

    WechatSession exchangeCode(String code);

    record WechatSession(String openId, String unionId, String sessionKey) {
    }
}
