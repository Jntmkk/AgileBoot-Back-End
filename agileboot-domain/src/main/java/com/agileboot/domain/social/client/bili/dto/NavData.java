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

    /**
     * WBI签名密钥（藏在假PNG URL里，需提取文件名作为img_key/sub_key）
     */
    private WbiImg wbiImg;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WbiImg {

        /** 假PNG URL，如 https://i0.hdslb.com/bfs/wbi/xxx.png */
        private String imgUrl;

        /** 假PNG URL，如 https://i0.hdslb.com/bfs/wbi/xxx.png */
        private String subUrl;
    }

}
