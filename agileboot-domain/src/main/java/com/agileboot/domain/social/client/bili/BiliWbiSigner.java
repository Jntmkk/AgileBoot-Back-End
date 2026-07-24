package com.agileboot.domain.social.client.bili;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.NavData;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Response;

/**
 * B站WBI签名器：从nav接口的wbi_img提取img_key/sub_key（缓存30分钟），计算w_rid和wts。
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

    /** WBI mixin permutation table（自2023年引入至今未变） */
    private static final int[] MIXIN_TABLE = {
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    };

    private volatile String cachedMixinKey;
    private volatile long cachedAt;
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);

    /**
     * 获取32位mixinKey（30分钟缓存）。
     * 调nav接口 → 从wbi_img.img_url/sub_url提取文件名 → 拼接后按mixin table取32位。
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
                // 用已登录账号调nav（需cookie才能拿到wbi_img）
                Response<BiliResp<NavData>> response = webApi.nav(2L).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("nav HTTP错误: {}", response.code());
                    return cachedMixinKey;
                }
                NavData navData = response.body().getData();
                if (navData == null || navData.getWbiImg() == null) {
                    log.warn("nav响应无wbi_img");
                    return cachedMixinKey;
                }
                String imgUrl = navData.getWbiImg().getImgUrl();
                String subUrl = navData.getWbiImg().getSubUrl();
                if (StrUtil.isBlank(imgUrl) || StrUtil.isBlank(subUrl)) {
                    log.warn("wbi_img URL为空");
                    return cachedMixinKey;
                }
                // 从URL中提取文件名（去掉路径和扩展名）
                String imgKey = extractKeyFromUrl(imgUrl);
                String subKey = extractKeyFromUrl(subUrl);
                log.debug("WBI keys: imgKey={}, subKey={}", imgKey, subKey);

                String raw = imgKey + subKey;
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

    /** 从URL路径中提取文件名（去掉路径前缀和扩展名） */
    private static String extractKeyFromUrl(String url) {
        if (url == null) {
            return "";
        }
        // e.g. https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png
        int lastSlash = url.lastIndexOf('/');
        String filename = lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    /**
     * 对params签名：添加w_rid(MD5)和wts(秒级时间戳)。
     * 签名后params中的值也被URL编码，配合@QueryMap(encoded=true)使用。
     */
    public void sign(Map<String, String> params) {
        String mixinKey = getMixinKey();
        if (StrUtil.isBlank(mixinKey)) {
            log.warn("WBI mixinKey为空，跳过签名");
            return;
        }
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            String encoded = URLEncoder.encode(entry.getValue());
            encoded = encoded.replace("+", "%20");
            // 过滤掉!'()*字符（B站WBI要求）
            encoded = encoded.replace("!", "%21").replace("'", "%27")
                .replace("(", "%28").replace(")", "%29").replace("*", "%2A");
            sb.append(entry.getKey()).append('=').append(encoded);
            // 同时更新params中的值为编码后的值，配合@QueryMap(encoded=true)
            params.put(entry.getKey(), encoded);
        }
        String signStr = sb + mixinKey;
        String wrid = MD5.create().digestHex(signStr);
        long wts = System.currentTimeMillis() / 1000;

        params.put("w_rid", wrid);
        params.put("wts", String.valueOf(wts));
    }

}
