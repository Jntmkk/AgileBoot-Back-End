package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * buvid 指纹（x/frontend/finger/spi 的 data 节点）。
 * 扫码流程必须先获取buvid携带请求，否则触发风控。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiData {

    @JsonProperty("b_3")
    private String buvid3;

    @JsonProperty("b_4")
    private String buvid4;

}
