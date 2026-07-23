package com.agileboot.domain.social.client;

import cn.hutool.json.JSONObject;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.client.dto.SocialLoginStatus;
import com.agileboot.domain.social.client.dto.SocialQrcode;
import com.agileboot.domain.social.client.dto.SocialUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 小红书平台客户端：{@link XhsApiClient} 的适配器，把上游 data 节点映射为跨平台通用DTO。
 * 上游字段为 snake_case（is_logged_in/red_id 等）。
 *
 * @author SocialMedia-Hub
 */
@Component
@RequiredArgsConstructor
public class XhsPlatformClient implements SocialPlatformClient {

    private final XhsApiClient xhsApiClient;

    @Override
    public String platform() {
        return "xhs";
    }

    @Override
    public SocialLoginStatus checkLoginStatus(SocialAccountEntity account) {
        JSONObject data = xhsApiClient.checkLoginStatus(account.getId());
        SocialLoginStatus status = new SocialLoginStatus();
        status.setIsLoggedIn(data.getBool("is_logged_in", false));
        status.setUsername(data.getStr("username"));
        return status;
    }

    @Override
    public SocialQrcode getLoginQrcode(SocialAccountEntity account) {
        JSONObject data = xhsApiClient.getLoginQrcode(account.getId());
        SocialQrcode qrcode = new SocialQrcode();
        qrcode.setImg(data.getStr("img"));
        qrcode.setTimeout(data.getInt("timeout"));
        qrcode.setIsLoggedIn(data.getBool("is_logged_in", false));
        return qrcode;
    }

    @Override
    public SocialUserProfile getMyProfile(SocialAccountEntity account) {
        JSONObject basicInfo = xhsApiClient.getMyProfile(account.getId());
        if (basicInfo == null) {
            return null;
        }
        // 小红书号字段上游为 redId/red_id 两种都兼容
        String redId = basicInfo.getStr("redId", basicInfo.getStr("red_id"));
        return new SocialUserProfile(redId, basicInfo.getStr("nickname"), basicInfo.getStr("imageb"));
    }

}
