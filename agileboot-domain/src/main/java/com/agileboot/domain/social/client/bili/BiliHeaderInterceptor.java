package com.agileboot.domain.social.client.bili;

import com.agileboot.domain.social.config.SocialMediaProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * B站固定请求头（UA + Referer），规避 -352 风控校验。
 *
 * @author SocialMedia-Hub
 */
@RequiredArgsConstructor
public class BiliHeaderInterceptor implements Interceptor {

    private final SocialMediaProperties properties;

    @Override
    public Response intercept(Chain chain) throws IOException {
        SocialMediaProperties.Bilibili config = properties.getBilibili();
        Request request = chain.request().newBuilder()
            .header("User-Agent", config.getUa())
            .header("Referer", config.getReferer())
            .build();
        return chain.proceed(request);
    }

}
