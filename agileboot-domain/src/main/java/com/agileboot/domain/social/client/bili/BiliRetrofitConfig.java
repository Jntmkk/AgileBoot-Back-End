package com.agileboot.domain.social.client.bili;

import com.agileboot.domain.social.config.SocialMediaProperties;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * B站Retrofit装配：两个baseUrl（passport/api）共享一个OkHttpClient（共享连接池）。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BiliRetrofitConfig {

    private final SocialMediaProperties properties;

    private final BiliCookieInterceptor cookieInterceptor;

    @Bean
    public OkHttpClient biliOkHttpClient() {
        SocialMediaProperties.Bilibili config = properties.getBilibili();
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return new OkHttpClient.Builder()
            .connectTimeout(config.getConnectTimeoutMs(), TimeUnit.MILLISECONDS)
            .readTimeout(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS)
            // cookie注入/捕获放最前，保证出网头与落库都不受后续拦截器影响
            .addInterceptor(cookieInterceptor)
            .addInterceptor(new BiliHeaderInterceptor(properties))
            .addInterceptor(logging)
            .build();
    }

    @Bean
    public BiliPassportApi biliPassportApi(OkHttpClient biliOkHttpClient) {
        return build(biliOkHttpClient, "https://passport.bilibili.com/", BiliPassportApi.class);
    }

    @Bean
    public BiliWebApi biliWebApi(OkHttpClient biliOkHttpClient) {
        return build(biliOkHttpClient, "https://api.bilibili.com/", BiliWebApi.class);
    }

    private <T> T build(OkHttpClient client, String baseUrl, Class<T> api) {
        return new Retrofit.Builder()
            .client(client)
            .baseUrl(baseUrl)
            .addConverterFactory(JacksonConverterFactory.create())
            .build()
            .create(api);
    }

}
