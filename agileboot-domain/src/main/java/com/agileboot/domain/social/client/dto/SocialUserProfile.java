package com.agileboot.domain.social.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社交平台账号资料（跨平台通用）。
 *
 * @author SocialMedia-Hub
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserProfile {

    /**
     * 平台侧用户ID（小红书号/B站mid）
     */
    private String platformUid;

    private String nickname;

    private String avatar;

}
