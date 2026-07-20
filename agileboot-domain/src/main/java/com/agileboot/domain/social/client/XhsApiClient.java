package com.agileboot.domain.social.client;

import cn.hutool.http.HttpException;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Internal;
import com.agileboot.domain.social.client.dto.XhsLoginStatus;
import com.agileboot.domain.social.client.dto.XhsQrcode;
import com.agileboot.domain.social.config.SocialMediaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 小红书账号容器 API 客户端（xiaohongshu-mcp 的 /api/v1/*）。
 * <p>
 * 地址解析：base-url + (portBase + 账号ID)，后端不感知账号所在节点。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XhsApiClient {

    private final SocialMediaProperties properties;

    /**
     * 账号容器地址，例如 http://frpc-visitor:18061
     */
    public String endpoint(Long accountId) {
        return properties.getBaseUrl() + ":" + (properties.getPortBase() + accountId.intValue());
    }

    public XhsLoginStatus checkLoginStatus(Long accountId) {
        JSONObject data = get(accountId, "/api/v1/login/status");
        return data.toBean(XhsLoginStatus.class);
    }

    public XhsQrcode getLoginQrcode(Long accountId) {
        JSONObject data = get(accountId, "/api/v1/login/qrcode");
        return data.toBean(XhsQrcode.class);
    }

    /**
     * 搜索笔记。返回原始 data 节点（结构随上游版本演进，先透传）。
     */
    public JSON searchFeeds(Long accountId, String keyword) {
        return get(accountId, "/api/v1/feeds/search?keyword=" + cn.hutool.core.util.URLUtil.encode(keyword));
    }

    /**
     * 获取首页推荐列表。返回原始 data 节点。
     */
    public JSON listFeeds(Long accountId) {
        return get(accountId, "/api/v1/feeds/list");
    }

    private JSONObject get(Long accountId, String path) {
        String url = endpoint(accountId) + path;
        String body;
        try {
            body = HttpUtil.get(url, properties.getHttpTimeoutMs());
        } catch (HttpException e) {
            log.warn("调用小红书账号容器失败 accountId={} url={}: {}", accountId, url, e.getMessage());
            throw new ApiException(Internal.INTERNAL_ERROR,
                "账号容器调用失败（可能节点离线）: " + e.getMessage());
        }
        return unwrap(accountId, url, body);
    }

    /**
     * 上游响应两种形态：
     * 成功 {"success":true,"data":{...},"message":"..."}
     * 失败 {"error":"...","code":"...","details":{}}
     */
    private JSONObject unwrap(Long accountId, String url, String body) {
        JSONObject json;
        try {
            json = JSONUtil.parseObj(body);
        } catch (Exception e) {
            throw new ApiException(Internal.INTERNAL_ERROR, "账号容器返回非JSON: " + body);
        }
        Boolean success = json.getBool("success");
        if (success == null || !success) {
            String error = json.getStr("error", json.getStr("message", "未知错误"));
            log.warn("小红书账号容器返回错误 accountId={} url={} error={}", accountId, url, error);
            throw new ApiException(Internal.INTERNAL_ERROR, "账号容器错误: " + error);
        }
        return json.getJSONObject("data");
    }

}
