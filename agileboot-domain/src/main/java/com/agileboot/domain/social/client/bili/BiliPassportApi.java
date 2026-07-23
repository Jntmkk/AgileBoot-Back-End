package com.agileboot.domain.social.client.bili;

import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.QrcodeGenerateData;
import com.agileboot.domain.social.client.bili.dto.QrcodePollData;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * B站通行证API（passport.bilibili.com）。
 * 每个方法首参为账号ID自定义头，由 {@link BiliCookieInterceptor} 读取后移除（绝不出网），
 * 并按账号注入cookie。
 *
 * @author SocialMedia-Hub
 */
public interface BiliPassportApi {

    String HEADER_ACCOUNT_ID = "X-Bili-Account-Id";

    /**
     * 申请登录二维码
     */
    @GET("x/passport-login/web/qrcode/generate")
    Call<BiliResp<QrcodeGenerateData>> generateQrcode(@Header(HEADER_ACCOUNT_ID) Long accountId);

    /**
     * 轮询扫码状态
     */
    @GET("x/passport-login/web/qrcode/poll")
    Call<BiliResp<QrcodePollData>> pollQrcode(@Header(HEADER_ACCOUNT_ID) Long accountId,
        @Query("qrcode_key") String qrcodeKey);

    // 预留：cookie刷新流程（需refresh_token + 对应签名，本次不实现）
    // @POST("x/passport-login/web/cookie/refresh")
    // Call<BiliResp<...>> refreshCookie(@Header(HEADER_ACCOUNT_ID) Long accountId, ...);

}
