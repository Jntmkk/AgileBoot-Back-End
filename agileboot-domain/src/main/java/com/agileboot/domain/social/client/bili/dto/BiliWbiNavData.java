package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * WBI签名密钥响应（wbi/index/nav）。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliWbiNavData {

    @JsonProperty("wbi_img")
    private WbiImg wbiImg;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WbiImg {

        @JsonProperty("img_key")
        private String imgKey;

        @JsonProperty("sub_key")
        private String subKey;
    }

}
