package com.agileboot.domain.social.client;

import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Business;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 平台客户端工厂：按 social_account.platform 分发到对应实现。
 *
 * @author SocialMedia-Hub
 */
@Component
@RequiredArgsConstructor
public class SocialPlatformClientFactory {

    private final List<SocialPlatformClient> clients;

    private final Map<String, SocialPlatformClient> clientMap = new HashMap<>();

    @PostConstruct
    public void init() {
        clients.forEach(client -> clientMap.put(client.platform(), client));
    }

    public SocialPlatformClient get(String platform) {
        SocialPlatformClient client = clientMap.get(platform);
        if (client == null) {
            throw new ApiException(Business.COMMON_UNSUPPORTED_OPERATION);
        }
        return client;
    }

}
