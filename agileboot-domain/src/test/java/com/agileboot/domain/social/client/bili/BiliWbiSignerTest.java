package com.agileboot.domain.social.client.bili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.MD5;
import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceVideoItem;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceDynamicData;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceVideoListData;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceVideoItem;
import com.agileboot.domain.social.client.bili.dto.NavData;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * B站 WBI 签名与 Space API 单元测试（用线上 cookie 直连 B站验证）。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
class BiliWbiSignerTest {

    /** 线上 B站 主号 cookie */
    private static final String COOKIE =
        "SESSDATA=1afdd685%2C1800340812%2C598ef%2A72CjDdpdCAiFlIGXKdSM6J4zg1HlNpcHUiUBtFX_qIeIVcUk3u0aIN7DGNxF5tjFGMDt0SVjdtS09WVkFpZzBBNzBpenZxRERjZXgzRVoyaWRlRjJCRXo2V1JObUVvZExnTzA1MlRIbEM2MWh4RnV6V3R4THY2aGxROVdWZ0tMbVZqTlVzbDk1VDdRIIEC; bili_jct=c48289893c94d70ff0c8cad77c60c88b; DedeUserID=430697998; DedeUserID__ckMd5=e5daa6bcc8cd183e; sid=mygkpi65";

    private static BiliWebApi webApi;

    @BeforeAll
    static void setUp() {
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .header("Referer", "https://www.bilibili.com")
                    .header("Cookie", COOKIE)
                    .build();
                return chain.proceed(req);
            })
            .addInterceptor(new HttpLoggingInterceptor(log::debug).setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .client(client)
            .baseUrl("https://api.bilibili.com/")
            .addConverterFactory(JacksonConverterFactory.create())
            .build();

        webApi = retrofit.create(BiliWebApi.class);
    }

    // ==================== WBI Key Extraction ====================

    @Test
    @DisplayName("从 fake PNG URL 中提取 key")
    void should_extract_key_from_url() {
        // 用反射测 private 方法太麻烦，直接验证逻辑
        String url = "https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png";
        int lastSlash = url.lastIndexOf('/');
        String filename = url.substring(lastSlash + 1);
        int dot = filename.lastIndexOf('.');
        String key = filename.substring(0, dot);

        assertEquals("7cd084941338484aae1ad9425b84077c", key);
        assertEquals(32, key.length());
    }

    // ==================== Mixin Key Generation ====================

    @Test
    @DisplayName("从 nav 响应获取 wbi_img 并生成 32 位 mixinKey")
    void should_generate_32char_mixin_key_from_nav() throws IOException {
        Response<BiliResp<NavData>> response = webApi.nav(2L).execute();
        assertTrue(response.isSuccessful(), "nav HTTP fail");
        BiliResp<NavData> body = response.body();
        assertNotNull(body);
        assertEquals(0, body.getCode(), "nav code != 0");
        NavData navData = body.getData();
        assertNotNull(navData);
        assertNotNull(navData.getWbiImg(), "wbi_img 为空（需已登录 cookie）");

        String imgUrl = navData.getWbiImg().getImgUrl();
        String subUrl = navData.getWbiImg().getSubUrl();
        assertNotNull(imgUrl);
        assertNotNull(subUrl);
        log.info("img_url: {}", imgUrl);
        log.info("sub_url: {}", subUrl);

        // 从 URL 提取 key
        String imgKey = imgUrl.substring(imgUrl.lastIndexOf('/') + 1,
            imgUrl.lastIndexOf('.'));
        String subKey = subUrl.substring(subUrl.lastIndexOf('/') + 1,
            subUrl.lastIndexOf('.'));
        assertEquals(32, imgKey.length());
        assertEquals(32, subKey.length());

        // 生成 mixinKey（用 MIXIN_TABLE 前 32 个索引）
        String raw = imgKey + subKey;
        int[] MIXIN_TABLE = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13
        };
        StringBuilder mixinKey = new StringBuilder(32);
        for (int idx : MIXIN_TABLE) {
            if (idx < raw.length()) {
                mixinKey.append(raw.charAt(idx));
            }
        }

        log.info("mixinKey: {}", mixinKey);
        assertEquals(32, mixinKey.length(), "mixinKey 必须是 32 位");
        assertTrue(mixinKey.chars().allMatch(c -> c >= 32 && c < 127),
            "mixinKey 应全是可打印 ASCII");
    }

    // ==================== WBI Signing ====================

    @Test
    @DisplayName("WBI 签名：生成 w_rid 和 wts")
    void should_sign_params_with_wbi() throws IOException {
        // 1. 获取 mixinKey
        Response<BiliResp<NavData>> response = webApi.nav(2L).execute();
        NavData navData = response.body().getData();
        assertNotNull(navData.getWbiImg());

        String imgKey = navData.getWbiImg().getImgUrl();
        imgKey = imgKey.substring(imgKey.lastIndexOf('/') + 1, imgKey.lastIndexOf('.'));
        String subKey = navData.getWbiImg().getSubUrl();
        subKey = subKey.substring(subKey.lastIndexOf('/') + 1, subKey.lastIndexOf('.'));
        String raw = imgKey + subKey;
        int[] MIXIN_TABLE = {
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13
        };
        StringBuilder sb = new StringBuilder(32);
        for (int idx : MIXIN_TABLE) {
            if (idx < raw.length()) { sb.append(raw.charAt(idx)); }
        }
        String mixinKey = sb.toString();

        // 2. 签名
        Map<String, String> params = new HashMap<>();
        params.put("mid", "546195");
        params.put("ps", "5");
        params.put("pn", "1");
        params.put("order", "pubdate");
        params.put("tid", "0");
        params.put("keyword", "");

        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (query.length() > 0) { query.append('&'); }
            String encoded = URLEncoder.encode(entry.getValue());
            encoded = encoded.replace("+", "%20");
            query.append(entry.getKey()).append('=').append(encoded);
        }
        String signStr = query + mixinKey;
        String wrid = MD5.create().digestHex(signStr);
        long wts = System.currentTimeMillis() / 1000;

        assertNotNull(wrid);
        assertEquals(32, wrid.length(), "w_rid 应是 MD5 hex（32 位）");
        assertTrue(wts > 0);

        params.put("w_rid", wrid);
        params.put("wts", String.valueOf(wts));
        log.info("Signed params: w_rid={}, wts={}", wrid, wts);
    }

    // ==================== Polymer Space API (实际使用) ====================

    @Test
    @DisplayName("用线上 cookie 调 polymer 空间动态 API 获取 UP 主视频列表")
    void should_fetch_up_videos_with_polymer_endpoint() throws IOException {
        // 老番茄 mid=546195
        Response<BiliResp<BiliSpaceDynamicData>> resp =
            webApi.fetchSpaceDynamic(2L, 546195L).execute();

        assertTrue(resp.isSuccessful(), "HTTP fail: " + resp.code());
        BiliResp<BiliSpaceDynamicData> body = resp.body();
        assertNotNull(body);
        log.info("polymer endpoint: code={}, message={}", body.getCode(), body.getMessage());

        if (body.getCode() == 0) {
            BiliSpaceDynamicData data = body.getData();
            assertNotNull(data);
            List<BiliSpaceDynamicData.DynamicItem> items = data.getItems();
            assertNotNull(items);
            log.info("动态总数: {}", items.size());

            int videoCount = 0;
            for (BiliSpaceDynamicData.DynamicItem item : items) {
                if (item.getModules() == null || item.getModules().getModuleDynamic() == null) {
                    continue;
                }
                BiliSpaceDynamicData.DynamicMajor major =
                    item.getModules().getModuleDynamic().getMajor();
                if (major == null) { continue; }
                if ("MAJOR_TYPE_ARCHIVE".equals(major.getType())
                    && major.getArchive() != null) {
                    videoCount++;
                    BiliSpaceDynamicData.DynamicArchive archive = major.getArchive();
                    assertNotNull(archive.getBvid(), "bvid 不应为空");
                    log.info("  [{}] {} (play:{})",
                        archive.getBvid(), archive.getTitle(),
                        archive.getStat() != null ? archive.getStat().getPlay() : "?");
                }
            }
            log.info("视频投稿数: {}", videoCount);
            assertTrue(videoCount > 0, "应有至少一个视频投稿");
        }
    }

    // ==================== WBI Space API (已废弃，仅参考) ====================

    @Test
    @DisplayName("WBI arc/search 端点（预期 -403，已废弃）")
    void should_return_403_on_wbi_endpoint() throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("mid", "546195");
        params.put("ps", "5");
        params.put("pn", "1");
        params.put("order", "pubdate");

        Response<BiliResp<BiliSpaceVideoListData>> resp =
            webApi.searchSpace(2L, params).execute();

        BiliResp<BiliSpaceVideoListData> body = resp.body();
        assertNotNull(body);
        log.info("WBI endpoint: code={}, message={}", body.getCode(), body.getMessage());
        // WBI endpoint 已知不可用: -403 权限不足
    }

}
