package com.agileboot.domain.social.client.bili;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Internal;
import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.BiliWbiNavData;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

/**
 * B站WBI签名器：缓存img_key/sub_key（30分钟），计算w_rid和wts。
 * <p>
 * 签名算法：params按key排序 → 拼接为k1=v1&k2=v2 →
 * 追加mixinKey → MD5 → 作为w_rid；wts取当前秒级时间戳。
 * <p>
 * 直接使用 BiliWebApi 避免与 BiliApiClient 循环依赖。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
public class BiliWbiSigner {

    private final BiliWebApi webApi;

    public BiliWbiSigner(BiliWebApi webApi) {
        this.webApi = webApi;
    }

    /**
     * WBI mixin table — 从img_key+sub_key拼接串中按此索引取32位形成mixin key。
     * B站不定期更新此表，若签名失败需更新。
     */
    private static final int[] MIXIN_TABLE = {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    /** 缓存的mixinKey */
    private volatile String cachedMixinKey;

    /** 缓存时间戳 */
    private volatile long cachedAt;

    /** 缓存有效期（30分钟），img_key/sub_key有效期约30分钟 */
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * 获取mixinKey（带30分钟缓存）。
     * 调wbi/index/nav接口获取img_key+sub_key，拼接后按mixin table取32位。
     */
    public String getMixinKey() {
        if (cachedMixinKey != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
            return cachedMixinKey;
        }
        synchronized (this) {
            if (cachedMixinKey != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MS) {
                return cachedMixinKey;
            }
            try {
                BiliWbiNavData navData = fetchWbiNav();
                if (navData == null || navData.getWbiImg() == null
                    || StrUtil.isBlank(navData.getWbiImg().getImgKey())
                    || StrUtil.isBlank(navData.getWbiImg().getSubKey())) {
                    log.warn("获取WBI密钥失败，navData={}", navData);
                    return cachedMixinKey; // 返回旧缓存兜底
                }
                String raw = navData.getWbiImg().getImgKey() + navData.getWbiImg().getSubKey();
                StringBuilder mixinKey = new StringBuilder(32);
                for (int idx : MIXIN_TABLE) {
                    if (idx < raw.length()) {
                        mixinKey.append(raw.charAt(idx));
                    }
                }
                cachedMixinKey = mixinKey.toString();
                cachedAt = System.currentTimeMillis();
                log.debug("WBI mixinKey已更新: {}", cachedMixinKey);
            } catch (Exception e) {
                log.warn("获取WBI密钥异常: {}", e.getMessage());
            }
            return cachedMixinKey;
        }
    }

    /**
     * 对params签名：添加w_rid(MD5(sortedParams+mixinKey))和wts(秒级时间戳)。
     *
     * @param params 原始参数（会被修改，添加w_rid和wts）
     */
    public void sign(Map<String, String> params) {
        String mixinKey = getMixinKey();
        if (StrUtil.isBlank(mixinKey)) {
            log.warn("WBI mixinKey为空，跳过签名");
            return;
        }
        // 排序
        TreeMap<String, String> sorted = new TreeMap<>(params);
        // 拼接 query string
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            // WBI签名时value需要URL编码，但空格不转+号
            String encoded = URLEncoder.encode(entry.getValue());
            // Java URLEncoder把空格编码为+，但B站WBI签名要求使用%20
            encoded = encoded.replace("+", "%20");
            sb.append(entry.getKey()).append('=').append(encoded);
        }
        String queryString = sb.toString();
        String signStr = queryString + mixinKey;
        String wrid = MD5.create().digestHex(signStr);
        long wts = System.currentTimeMillis() / 1000;

        params.put("w_rid", wrid);
        params.put("wts", String.valueOf(wts));
    }

    private BiliWbiNavData fetchWbiNav() {
        try {
            Response<BiliResp<BiliWbiNavData>> response = webApi.wbiNav(0L).execute();
            if (!response.isSuccessful()) {
                log.warn("wbi/index/nav HTTP错误: {}", response.code());
                return null;
            }
            BiliResp<BiliWbiNavData> body = response.body();
            if (body == null || !body.success() || body.getData() == null) {
                log.warn("wbi/index/nav 业务错误: code={}, message={}",
                    body != null ? body.getCode() : null, body != null ? body.getMessage() : null);
                return null;
            }
            return body.getData();
        } catch (IOException e) {
            log.warn("wbi/index/nav 调用失败: {}", e.getMessage());
            return null;
        }
    }

}
