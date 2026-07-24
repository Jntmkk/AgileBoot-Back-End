package com.agileboot.domain.social.client.bili;

import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceVideoListData;
import com.agileboot.domain.social.client.bili.dto.BiliWbiNavData;
import com.agileboot.domain.social.client.bili.dto.NavData;
import com.agileboot.domain.social.client.bili.dto.SpiData;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.QueryMap;

/**
 * B站主站API（api.bilibili.com）。
 * 扩展预留：投稿/评论等web API都加在这里，每个方法第一个参数都是账号ID头。
 * 需要wbi签名的接口届时加 WbiSignInterceptor 统一处理。
 *
 * @author SocialMedia-Hub
 */
public interface BiliWebApi {

    /**
     * 获取buvid3/buvid4指纹（扫码流程必须先获取携带，否则触发风控）
     */
    @GET("x/frontend/finger/spi")
    Call<BiliResp<SpiData>> spi(@Header(BiliPassportApi.HEADER_ACCOUNT_ID) Long accountId);

    /**
     * 导航栏用户信息（登录态校验 + 昵称/mid/头像）
     */
    @GET("x/web-interface/nav")
    Call<BiliResp<NavData>> nav(@Header(BiliPassportApi.HEADER_ACCOUNT_ID) Long accountId);

    /**
     * WBI签名密钥（img_key + sub_key，约30分钟有效）
     */
    @GET("x/web-interface/wbi/index/nav")
    Call<BiliResp<BiliWbiNavData>> wbiNav(@Header(BiliPassportApi.HEADER_ACCOUNT_ID) Long accountId);

    /**
     * UP主空间投稿搜索（旧版接口，无需WBI签名）。
     */
    @GET("x/space/arc/search")
    Call<BiliResp<BiliSpaceVideoListData>> searchSpace(
        @Header(BiliPassportApi.HEADER_ACCOUNT_ID) Long accountId,
        @QueryMap(encoded = true) Map<String, String> params);

}
