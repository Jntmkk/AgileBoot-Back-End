package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * B站API统一响应包裹。
 * code：0成功；-101未登录；-352风控校验失败；-400参数错误。
 * 注意扫码轮询的 data.code（86101/86090/86038）是业务状态，与本code无关。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliResp<T> {

    private Integer code;

    private String message;

    private T data;

    public boolean success() {
        return Integer.valueOf(0).equals(code);
    }

}
