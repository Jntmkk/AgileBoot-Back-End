package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 导航栏用户信息（x/web-interface/nav 的 data 节点）。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavData {

    private Boolean isLogin;

    /**
     * 用户ID（mid）
     */
    private Long mid;

    /**
     * 昵称
     */
    private String uname;

    /**
     * 头像url
     */
    private String face;

}
