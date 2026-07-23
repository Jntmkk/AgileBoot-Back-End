package com.agileboot.domain.social.client.bili;

import cn.hutool.core.util.StrUtil;
import com.agileboot.domain.social.credential.BiliCookieStore;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Component;

/**
 * 多账号cookie拦截器（核心）。
 * <p>
 * 不用OkHttp CookieJar：共享client按host索引会跨账号串cookie。
 * 这里按账号显式注入/捕获：
 * <ul>
 *   <li>请求：读 X-Bili-Account-Id 头（随后移除，绝不出网），从 {@link BiliCookieStore} 拼Cookie注入</li>
 *   <li>响应：捕获 Set-Cookie（扫码成功时下发 SESSDATA/bili_jct/DedeUserID），合并落库</li>
 * </ul>
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiliCookieInterceptor implements Interceptor {

    private final BiliCookieStore cookieStore;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String accountHeader = original.header(BiliPassportApi.HEADER_ACCOUNT_ID);
        Request.Builder builder = original.newBuilder()
            .removeHeader(BiliPassportApi.HEADER_ACCOUNT_ID);

        Long accountId = accountHeader == null ? null : Long.valueOf(accountHeader);
        if (accountId != null) {
            String cookie = cookieStore.buildCookieHeader(accountId);
            if (StrUtil.isNotBlank(cookie)) {
                builder.header("Cookie", cookie);
            }
        }
        Response response = chain.proceed(builder.build());

        List<String> setCookies = response.headers("Set-Cookie");
        if (accountId != null && !setCookies.isEmpty()) {
            try {
                cookieStore.onResponseCookies(accountId, setCookies);
            } catch (Exception e) {
                // cookie落库失败不影响本次请求结果
                log.warn("B站账号 {} Set-Cookie落库失败: {}", accountId, e.getMessage());
            }
        }
        return response;
    }

}
