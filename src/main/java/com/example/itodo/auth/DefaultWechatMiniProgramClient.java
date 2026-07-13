package com.example.itodo.auth;

import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@EnableConfigurationProperties(WechatMiniProgramProperties.class)
public class DefaultWechatMiniProgramClient implements WechatMiniProgramClient {

    private static final String CODE_TO_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final WechatMiniProgramProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DefaultWechatMiniProgramClient(WechatMiniProgramProperties properties,
                                          RestClient.Builder restClientBuilder,
                                          ObjectMapper objectMapper) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public WechatSession exchangeCode(String code) {
        if (!StringUtils.hasText(properties.appId()) || !StringUtils.hasText(properties.secret())) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "WeChat mini-program credentials are not configured");
        }

        // WeChat returns Content-Type "text/plain" even though the body is JSON,
        // so read it as a raw String first and parse with Jackson manually.
        String body = restClient.get()
                .uri(CODE_TO_SESSION_URL + "?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code",
                        properties.appId(), properties.secret(), code)
                .retrieve()
                .body(String.class);

        if (!StringUtils.hasText(body)) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "WeChat mini-program returned empty response");
        }

        CodeToSessionResponse response;
        try {
            response = objectMapper.readValue(body, CodeToSessionResponse.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "Failed to parse WeChat response", e);
        }

        if (response == null || !StringUtils.hasText(response.openid())) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "WeChat mini-program login failed");
        }
        if (response.errcode() != null && response.errcode() != 0) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "WeChat mini-program login failed");
        }
        return new WechatSession(response.openid(), response.unionid(), response.session_key());
    }

    private record CodeToSessionResponse(
            String openid,
            String unionid,
            String session_key,
            Integer errcode,
            String errmsg
    ) {
    }
}
