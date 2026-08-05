package com.agileboot.domain.social.follow;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.account.db.SocialAccountService;
import com.agileboot.domain.social.config.SocialMediaProperties;
import com.agileboot.domain.social.credential.BiliCookieStore;
import com.agileboot.domain.social.follow.command.BackfillCommand;
import com.agileboot.domain.social.follow.command.SyncByLinkCommand;
import com.agileboot.domain.social.follow.db.SocialFollowUpEntity;
import com.agileboot.domain.social.follow.db.SocialFollowUpService;
import com.agileboot.domain.social.post.db.SocialSyncPostEntity;
import com.agileboot.domain.social.post.db.SocialSyncPostService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * B站动态同步服务：从 polymer/space 抓取 UP 主动态并在后端写入 social_sync_post。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BiliSyncService {

    private static final String PLATFORM = "bili";
    private static final String FEED_URL = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/space";
    private static final String DETAIL_URL = "https://api.bilibili.com/x/polymer/web-dynamic/v1/detail";
    private static final String VIEW_URL = "https://api.bilibili.com/x/web-interface/view";

    private static final Pattern BVID_PATTERN = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final Pattern DYNAMIC_ID_PATTERN = Pattern.compile("(?:dynamic|opus)/([0-9]+)|t\\.bilibili\\.com/([0-9]+)");

    private final SocialMediaProperties properties;
    private final SocialFollowUpService followUpService;
    private final SocialSyncPostService postService;
    private final SocialAccountService accountService;
    private final BiliCookieStore cookieStore;

    private String getSyncCookie() {
        // 优先使用 social_account + social_credential 里有效的 B站登录凭据
        List<SocialAccountEntity> biliAccounts = accountService.lambdaQuery()
            .eq(SocialAccountEntity::getPlatform, PLATFORM)
            .eq(SocialAccountEntity::getStatus, 1)
            .eq(SocialAccountEntity::getDeleted, false)
            .list();
        for (SocialAccountEntity account : biliAccounts) {
            if (cookieStore.hasValidCredential(account.getId())) {
                log.debug("使用账号 {} 的B站cookie同步", account.getId());
                return cookieStore.buildCookieHeader(account.getId());
            }
        }
        // 兜底：环境变量
        String envCookie = properties.getBilibili().getSyncCookie();
        if (StrUtil.isNotBlank(envCookie)) {
            log.debug("使用环境变量 BILI_SYNC_COOKIE");
        }
        return envCookie;
    }

    public void syncFeed() {
        List<SocialFollowUpEntity> ups = listEnabledUps();
        if (CollUtil.isEmpty(ups)) {
            log.info("没有启用的B站同步UP，跳过 feed 同步");
            return;
        }
        for (SocialFollowUpEntity up : ups) {
            try {
                PageFetchResult result = fetchPage(up.getUpId(), null);
                int saved = savePosts(result.posts);
                log.info("UP {} feed 同步完成，本页{}条，已保存{}条，hasMore={}",
                    up.getUpId(), result.posts.size(), saved, result.hasMore);
                up.setLastSyncAt(new Date());
                followUpService.updateById(up);
            } catch (Exception e) {
                log.error("UP {} feed 同步失败", up.getUpId(), e);
            }
        }
    }

    public void backfill(BackfillCommand command) {
        String platform = command.getPlatform() == null ? PLATFORM : command.getPlatform();
        if (!PLATFORM.equals(platform)) {
            log.warn("暂不支持平台 {}", platform);
            return;
        }
        List<SocialFollowUpEntity> ups;
        if (StrUtil.isNotBlank(command.getUpId())) {
            SocialFollowUpEntity up = getEnabledUpByUpId(command.getUpId());
            if (up == null) {
                throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID,
                    "未找到指定的启用同步UP: " + command.getUpId());
            }
            ups = CollUtil.newArrayList(up);
        } else {
            ups = listEnabledUps();
            if (CollUtil.isEmpty(ups)) {
                throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "没有启用的同步UP");
            }
        }
        Date start = command.getStartTime();
        Date end = command.getEndTime();
        for (SocialFollowUpEntity up : ups) {
            try {
                int saved = backfillUp(up.getUpId(), start, end);
                log.info("UP {} 补数据完成，保存{}条", up.getUpId(), saved);
                up.setLastSyncAt(new Date());
                followUpService.updateById(up);
            } catch (Exception e) {
                log.error("UP {} 补数据失败", up.getUpId(), e);
            }
        }
    }

    public void syncByLink(SyncByLinkCommand command) {
        if (!PLATFORM.equals(command.getPlatform())) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "仅支持 bili 平台");
        }
        String url = command.getUrl();
        Matcher bvidMatcher = BVID_PATTERN.matcher(url);
        if (bvidMatcher.find()) {
            saveVideoByBvid(bvidMatcher.group());
            return;
        }
        if (url.contains("/video/")) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "无法从视频链接解析BV号");
        }
        if (url.contains("/dynamic/") || url.contains("/opus/") || url.contains("t.bilibili.com")) {
            Matcher m = DYNAMIC_ID_PATTERN.matcher(url);
            if (!m.find()) {
                throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "无法从链接解析动态ID");
            }
            String dynamicId = m.group(1) != null ? m.group(1) : m.group(2);
            saveDynamicById(dynamicId);
            return;
        }
        throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "不支持的B站链接");
    }

    private List<SocialFollowUpEntity> listEnabledUps() {
        return followUpService.lambdaQuery()
            .eq(SocialFollowUpEntity::getPlatform, PLATFORM)
            .eq(SocialFollowUpEntity::getStatus, 1)
            .eq(SocialFollowUpEntity::getSyncEnabled, 1)
            .eq(SocialFollowUpEntity::getDeleted, false)
            .list();
    }

    private SocialFollowUpEntity getEnabledUpByUpId(String upId) {
        return followUpService.lambdaQuery()
            .eq(SocialFollowUpEntity::getPlatform, PLATFORM)
            .eq(SocialFollowUpEntity::getUpId, upId)
            .eq(SocialFollowUpEntity::getStatus, 1)
            .eq(SocialFollowUpEntity::getSyncEnabled, 1)
            .eq(SocialFollowUpEntity::getDeleted, false)
            .one();
    }

    private PageFetchResult fetchPage(String hostMid, String offset) {
        String cookie = getSyncCookie();
        if (StrUtil.isBlank(cookie)) {
            log.warn("BILI_SYNC_COOKIE 未配置，跳过B站同步");
            return new PageFetchResult();
        }
        HttpRequest request = buildFeedRequest(hostMid, offset, cookie);
        String body;
        try (HttpResponse response = request.execute()) {
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new ApiException(ErrorCode.FAILED,
                    "B站动态接口HTTP错误(" + response.getStatus() + ")");
            }
            body = response.body();
        }
        JSONObject resp = JSONUtil.parseObj(body);
        Integer code = resp.getInt("code");
        if (code == null || code != 0) {
            throw new ApiException(ErrorCode.FAILED,
                "B站动态接口错误(" + code + "): " + resp.getStr("message"));
        }
        JSONObject data = resp.getJSONObject("data");
        boolean hasMore = Boolean.TRUE.equals(data.getBool("has_more"));
        String nextOffset = data.getStr("offset");
        JSONArray items = data.getJSONArray("items");
        List<SocialSyncPostEntity> posts = new ArrayList<>();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                SocialSyncPostEntity post = parseDynamic(items.getJSONObject(i));
                if (post != null) {
                    posts.add(post);
                }
            }
        }
        PageFetchResult result = new PageFetchResult();
        result.posts = posts;
        result.hasMore = hasMore;
        result.nextOffset = nextOffset;
        return result;
    }

    private HttpRequest buildFeedRequest(String hostMid, String offset, String cookie) {
        StringBuilder url = new StringBuilder(FEED_URL);
        url.append("?features=itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard");
        url.append("&host_mid=").append(hostMid);
        if (StrUtil.isNotBlank(offset)) {
            url.append("&offset=").append(offset);
        }
        url.append("&ps=").append(properties.getBilibili().getSyncPageSize());
        return HttpUtil.createGet(url.toString())
            .header("User-Agent", properties.getBilibili().getUa())
            .header("Referer", "https://space.bilibili.com/" + hostMid)
            .header("Origin", "https://space.bilibili.com")
            .header("Accept", "application/json, text/plain, */*")
            .header("x-bili-device-req-json", "{\"platform\":\"web\",\"device\":\"pc\"}")
            .header("x-bili-web-req-json", "{\"spm_id\":\"333.1387\"}")
            .header("Cookie", cookie)
            .timeout(properties.getBilibili().getReadTimeoutMs());
    }

    private void saveDynamicById(String dynamicId) {
        String cookie = getSyncCookie();
        if (StrUtil.isBlank(cookie)) {
            throw new ApiException(ErrorCode.FAILED, "BILI_SYNC_COOKIE 未配置");
        }
        StringBuilder url = new StringBuilder(DETAIL_URL);
        url.append("?platform=web&timezone=-480&id=").append(dynamicId);
        url.append("&features=itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote,forwardListHidden,decorationCard,commentsNewVersion,onlyfansAssetsV2,ugcDelete,onlyfansQaCard");
        String body = executeGet(url.toString(), cookie);
        JSONObject resp = JSONUtil.parseObj(body);
        Integer code = resp.getInt("code");
        if (code == null || code != 0) {
            throw new ApiException(ErrorCode.FAILED,
                "B站动态详情接口错误(" + code + "): " + resp.getStr("message"));
        }
        JSONObject item = resp.getJSONObject("data");
        if (item != null) {
            item = item.getJSONObject("item");
        }
        if (item == null) {
            throw new ApiException(ErrorCode.FAILED, "B站动态详情未返回 item");
        }
        SocialSyncPostEntity post = parseDynamic(item);
        if (post != null) {
            savePost(post);
        }
    }

    private void saveVideoByBvid(String bvid) {
        String cookie = getSyncCookie();
        StringBuilder url = new StringBuilder(VIEW_URL);
        url.append("?bvid=").append(bvid);
        String body = executeGet(url.toString(), cookie);
        JSONObject resp = JSONUtil.parseObj(body);
        Integer code = resp.getInt("code");
        if (code == null || code != 0) {
            throw new ApiException(ErrorCode.FAILED,
                "B站视频详情接口错误(" + code + "): " + resp.getStr("message"));
        }
        JSONObject data = resp.getJSONObject("data");
        SocialSyncPostEntity post = new SocialSyncPostEntity();
        post.setPlatform(PLATFORM);
        post.setPlatformPostId(bvid);
        post.setPostType(2);
        post.setTitle(data.getStr("title"));
        post.setContent(data.getStr("desc"));
        post.setCoverUrl(data.getStr("pic"));
        post.setVideoUrl("https://www.bilibili.com/video/" + bvid);
        post.setPlatformPostUrl(post.getVideoUrl());
        JSONObject owner = data.getJSONObject("owner");
        if (owner != null) {
            post.setPlatformUserId(String.valueOf(owner.getLong("mid")));
            post.setNickname(owner.getStr("name"));
        }
        Long pubdate = data.getLong("pubdate");
        if (pubdate != null) {
            post.setPublishedAt(new Date(pubdate * 1000L));
        }
        JSONObject stat = data.getJSONObject("stat");
        if (stat != null) {
            post.setReadCount(parseCount(stat.getStr("view")));
            post.setLikeCount(parseCount(stat.getStr("like")));
            post.setCommentCount(parseCount(stat.getStr("reply")));
            post.setShareCount(parseCount(stat.getStr("share")));
            post.setCoinCount(parseCount(stat.getStr("coin")));
        }
        post.setAudioStatus(1);
        post.setSyncedAt(new Date());
        post.setStatus(1);
        post.setCreatorId(1L);
        post.setUpdaterId(1L);
        savePost(post);
    }

    private int backfillUp(String hostMid, Date start, Date end) {
        String offset = null;
        boolean hasMore = true;
        int totalSaved = 0;
        while (hasMore) {
            PageFetchResult result = fetchPage(hostMid, offset);
            hasMore = result.hasMore;
            offset = result.nextOffset;
            boolean sawOld = false;
            for (SocialSyncPostEntity post : result.posts) {
                Date pub = post.getPublishedAt();
                if (pub == null) {
                    continue;
                }
                if (pub.before(start)) {
                    sawOld = true;
                    continue;
                }
                if (!pub.after(end)) {
                    if (savePost(post)) {
                        totalSaved++;
                    }
                }
            }
            if (sawOld) {
                break;
            }
            if (CollUtil.isEmpty(result.posts)) {
                break;
            }
        }
        return totalSaved;
    }

    private int savePosts(List<SocialSyncPostEntity> posts) {
        int saved = 0;
        for (SocialSyncPostEntity post : posts) {
            if (savePost(post)) {
                saved++;
            }
        }
        return saved;
    }

    private boolean savePost(SocialSyncPostEntity post) {
        if (post == null || StrUtil.isBlank(post.getPlatformPostId())) {
            return false;
        }
        boolean exists = postService.lambdaQuery()
            .eq(SocialSyncPostEntity::getPlatform, post.getPlatform())
            .eq(SocialSyncPostEntity::getPlatformPostId, post.getPlatformPostId())
            .count() > 0;
        if (exists) {
            return false;
        }
        postService.save(post);
        return true;
    }

    private SocialSyncPostEntity parseDynamic(JSONObject dynamic) {
        String idStr = dynamic.getStr("id_str");
        if (StrUtil.isBlank(idStr)) {
            return null;
        }
        JSONObject modules = dynamic.getJSONObject("modules");
        if (modules == null) {
            return null;
        }
        JSONObject author = modules.getJSONObject("module_author");
        JSONObject moduleDynamic = modules.getJSONObject("module_dynamic");
        if (moduleDynamic == null) {
            return null;
        }
        JSONObject major = moduleDynamic.getJSONObject("major");
        JSONObject desc = moduleDynamic.getJSONObject("desc");

        SocialSyncPostEntity post = new SocialSyncPostEntity();
        post.setPlatform(PLATFORM);
        post.setPlatformPostId(idStr);
        post.setPlatformPostUrl("https://t.bilibili.com/" + idStr);
        if (author != null) {
            post.setPlatformUserId(author.getStr("mid"));
            post.setNickname(author.getStr("name"));
            Long pubTs = author.getLong("pub_ts");
            if (pubTs != null) {
                post.setPublishedAt(new Date(pubTs * 1000L));
            }
        }
        post.setSyncedAt(new Date());
        post.setStatus(1);
        post.setCreatorId(1L);
        post.setUpdaterId(1L);
        post.setRawMeta(dynamic.toString());

        if (major == null) {
            post.setPostType(1);
            post.setContent(desc == null ? null : desc.getStr("text"));
            post.setAudioStatus(0);
            return post;
        }

        JSONObject archive = major.getJSONObject("archive");
        JSONObject opus = major.getJSONObject("opus");
        JSONObject draw = major.getJSONObject("draw");

        if (archive != null && StrUtil.isNotBlank(archive.getStr("bvid"))) {
            post.setPostType(2);
            post.setTitle(archive.getStr("title"));
            post.setCoverUrl(archive.getStr("cover"));
            post.setVideoUrl("https://www.bilibili.com/video/" + archive.getStr("bvid"));
            post.setContent(StrUtil.blankToDefault(archive.getStr("desc"),
                desc == null ? null : desc.getStr("text")));
            post.setAudioStatus(1);
        } else if (hasImages(opus) || hasImages(draw)) {
            post.setPostType(1);
            post.setTitle(opus == null ? null : opus.getStr("title"));
            JSONArray pics = hasImages(opus) ? opus.getJSONArray("pics") : draw.getJSONArray("items");
            List<String> images = new ArrayList<>();
            if (pics != null) {
                for (int i = 0; i < pics.size(); i++) {
                    JSONObject p = pics.getJSONObject(i);
                    if (p != null) {
                        images.add(StrUtil.blankToDefault(p.getStr("url"), p.getStr("src")));
                    }
                }
            }
            post.setImages(JSONUtil.toJsonStr(images));
            if (StrUtil.isBlank(post.getCoverUrl()) && !images.isEmpty()) {
                post.setCoverUrl(images.get(0));
            }
            String opusSummary = null;
            if (opus != null) {
                JSONObject summary = opus.getJSONObject("summary");
                if (summary != null) {
                    opusSummary = summary.getStr("text");
                }
            }
            post.setContent(StrUtil.blankToDefault(opusSummary,
                desc == null ? null : desc.getStr("text")));
            post.setAudioStatus(0);
        } else if (opus != null && StrUtil.isNotBlank(getOpusSummaryText(opus))) {
            post.setPostType(1);
            post.setTitle(opus.getStr("title"));
            post.setContent(getOpusSummaryText(opus));
            post.setAudioStatus(0);
        } else {
            post.setPostType(1);
            post.setContent(desc == null ? null : desc.getStr("text"));
            post.setAudioStatus(0);
        }
        return post;
    }

    private String getOpusSummaryText(JSONObject opus) {
        if (opus == null) {
            return null;
        }
        JSONObject summary = opus.getJSONObject("summary");
        if (summary == null) {
            return null;
        }
        return summary.getStr("text");
    }

    private boolean hasImages(JSONObject obj) {
        if (obj == null) {
            return false;
        }
        JSONArray arr = obj.getJSONArray("pics");
        if (arr != null && !arr.isEmpty()) {
            return true;
        }
        arr = obj.getJSONArray("items");
        return arr != null && !arr.isEmpty();
    }

    private String executeGet(String url, String cookie) {
        HttpRequest request = HttpUtil.createGet(url)
            .header("User-Agent", properties.getBilibili().getUa())
            .header("Referer", "https://www.bilibili.com")
            .header("Origin", "https://www.bilibili.com")
            .header("Accept", "application/json, text/plain, */*")
            .header("x-bili-device-req-json", "{\"platform\":\"web\",\"device\":\"pc\"}")
            .header("x-bili-web-req-json", "{\"spm_id\":\"333.1387\"}");
        if (StrUtil.isNotBlank(cookie)) {
            request.header("Cookie", cookie);
        }
        request.timeout(properties.getBilibili().getReadTimeoutMs());
        try (HttpResponse response = request.execute()) {
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                throw new ApiException(ErrorCode.FAILED,
                    "B站接口HTTP错误(" + response.getStatus() + "): " + url);
            }
            return response.body();
        }
    }

    private Integer parseCount(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class PageFetchResult {
        List<SocialSyncPostEntity> posts = new ArrayList<>();
        boolean hasMore;
        String nextOffset;
    }
}
