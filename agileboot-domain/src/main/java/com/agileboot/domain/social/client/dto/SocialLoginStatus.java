package com.agileboot.domain.social.client.dto;

import lombok.Data;

/**
 * 社交平台登录状态（跨平台通用）。
 *
 * @author SocialMedia-Hub
 */
@Data
public class SocialLoginStatus {

    private Boolean isLoggedIn;

    private String username;

    /**
     * 真实账号昵称（登录时由平台资料接口补充）
     */
    private String nickname;

    /**
     * 平台侧用户ID（小红书号/B站mid，登录时由平台资料接口补充）
     */
    private String platformUid;

    /**
     * 扫码中间态提示（如"已扫码，请在手机上确认"/"二维码已过期"），无中间态时为null
     */
    private String qrStatus;

}
