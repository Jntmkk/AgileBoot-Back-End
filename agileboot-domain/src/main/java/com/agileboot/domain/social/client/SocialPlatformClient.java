package com.agileboot.domain.social.client;

import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.client.dto.SocialLoginStatus;
import com.agileboot.domain.social.client.dto.SocialQrcode;
import com.agileboot.domain.social.client.dto.SocialUserProfile;

/**
 * 社交平台客户端（按 social_account.platform 策略分发）。
 * <p>
 * 平台特有操作（如小红书搜笔记）不进本接口，由应用服务对具体平台单独处理。
 *
 * @author SocialMedia-Hub
 */
public interface SocialPlatformClient {

    /**
     * 平台标识（xhs/bili），与 social_account.platform 对应
     */
    String platform();

    /**
     * 查询账号实时登录状态
     */
    SocialLoginStatus checkLoginStatus(SocialAccountEntity account);

    /**
     * 获取扫码登录二维码
     */
    SocialQrcode getLoginQrcode(SocialAccountEntity account);

    /**
     * 获取账号资料（昵称/平台UID/头像）。未登录或获取失败返回null
     */
    SocialUserProfile getMyProfile(SocialAccountEntity account);

}
