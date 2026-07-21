package com.agileboot.domain.social.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * 小红书登录状态（对应 /api/v1/login/status 的 data 节点）。
 * 上游为 snake_case，对外序列化为 camelCase（与系统其他 API 风格一致）。
 *
 * @author SocialMedia-Hub
 */
@Data
public class XhsLoginStatus {

    @JsonAlias("is_logged_in")
    private Boolean isLoggedIn;

    private String username;

}
