package com.example.itodo.auth;

import com.example.itodo.common.error.BusinessException;
import com.example.itodo.common.error.ErrorCode;
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

    public DefaultWechatMiniProgramClient(WechatMiniProgramProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public WechatSession exchangeCode(String code) {
        if (!StringUtils.hasText(properties.appId()) || !StringUtils.hasText(properties.secret())) {
            throw new BusinessException(ErrorCode.EXTERNAL_AUTH_FAILED, "WeChat mini-program credentials are not configured");
        }

        CodeToSessionResponse response = restClient.get()
                .uri(CODE_TO_SESSION_URL + "?appid={appId}&secret={secret}&js_code={code}&grant_type=authorization_code",
                        properties.appId(), properties.secret(), code)
                .retrieve()
                .body(CodeToSessionResponse.class);

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
