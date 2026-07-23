package com.agileboot.domain.social.credential;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * B站扫码登录会话（临时态，存Redis，TTL=二维码有效期）。
 * 扫码期间cookie只有buvid3/buvid4（B站风控要求携带buvid才能走完扫码）。
 *
 * @author SocialMedia-Hub
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BiliLoginSession {

    private String qrcodeKey;

    private String buvid3;

    private String buvid4;

}
