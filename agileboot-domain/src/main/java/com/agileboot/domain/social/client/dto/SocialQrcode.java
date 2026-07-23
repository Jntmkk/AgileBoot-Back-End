package com.agileboot.domain.social.client.dto;

import lombok.Data;

/**
 * 社交平台登录二维码（跨平台通用）。
 * 各平台统一由后端渲染为 base64 图片，前端协议不变。
 *
 * @author SocialMedia-Hub
 */
@Data
public class SocialQrcode {

    /**
     * base64 图片（可能带 data:image 前缀）
     */
    private String img;

    /**
     * 二维码有效期（秒）
     */
    private Integer timeout;

    private Boolean isLoggedIn;

    public SocialQrcode() {
    }

    public SocialQrcode(String img, Integer timeout, Boolean isLoggedIn) {
        this.img = img;
        this.timeout = timeout;
        this.isLoggedIn = isLoggedIn;
    }

    /**
     * 已是登录态，无需扫码
     */
    public static SocialQrcode alreadyLoggedIn() {
        return new SocialQrcode(null, 0, true);
    }

}
