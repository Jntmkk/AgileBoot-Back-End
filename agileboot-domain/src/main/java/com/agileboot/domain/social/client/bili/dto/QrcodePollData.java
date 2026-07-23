package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 扫码登录-轮询 响应（qrcode/poll 的 data 节点）。
 * code：0登录成功；86101未扫描；86090已扫描待确认；86038二维码已过期。
 * 登录成功时响应 Set-Cookie 含 SESSDATA/bili_jct/DedeUserID 等（由拦截器统一捕获落库）。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QrcodePollData {

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_NOT_SCANNED = 86101;
    public static final int CODE_SCANNED_PENDING = 86090;
    public static final int CODE_EXPIRED = 86038;

    private Integer code;

    private String message;

    /**
     * 登录成功时的跳转url（query里同样携带cookie值，以Set-Cookie为准，不解析）
     */
    private String url;

    @JsonProperty("refresh_token")
    private String refreshToken;

}
