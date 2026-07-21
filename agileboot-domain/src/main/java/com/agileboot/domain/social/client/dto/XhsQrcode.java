package com.agileboot.domain.social.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 小红书登录二维码（对应 /api/v1/login/qrcode 的 data 节点）。
 * 上游为 snake_case，对外序列化为 camelCase（与系统其他 API 风格一致）。
 *
 * @author SocialMedia-Hub
 */
@Data
public class XhsQrcode {

    /**
     * base64 图片（可能带 data:image 前缀）
     */
    private String img;

    /**
     * 二维码有效期（秒）
     */
    private Integer timeout;

    @JsonAlias("is_logged_in")
    private Boolean isLoggedIn;

}
