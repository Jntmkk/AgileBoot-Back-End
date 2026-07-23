package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 扫码登录-申请二维码 响应（qrcode/generate 的 data 节点）。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QrcodeGenerateData {

    /**
     * 二维码内容文本（需渲染成图片）
     */
    private String url;

    @JsonProperty("qrcode_key")
    private String qrcodeKey;

}
