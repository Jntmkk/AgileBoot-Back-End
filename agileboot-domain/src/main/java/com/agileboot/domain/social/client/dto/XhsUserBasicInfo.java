package com.agileboot.domain.social.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 小红书"我的主页"基本信息（/api/v1/user/me 的 data.data.userBasicInfo 节点）
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class XhsUserBasicInfo {

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 小红书号
     */
    private String redId;

    /**
     * 头像（大）
     */
    private String imageb;

    /**
     * 头像（小）
     */
    private String images;

    /**
     * 简介
     */
    private String desc;

}
