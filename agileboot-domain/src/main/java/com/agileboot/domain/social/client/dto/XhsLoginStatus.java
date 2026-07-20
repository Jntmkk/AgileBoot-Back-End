package com.agileboot.domain.social.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 小红书登录状态（对应 /api/v1/login/status 的 data 节点）
 *
 * @author SocialMedia-Hub
 */
@Data
public class XhsLoginStatus {

    @JsonProperty("is_logged_in")
    private Boolean isLoggedIn;

    private String username;

}
