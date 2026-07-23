package com.agileboot.domain.social.credential;

import cn.hutool.core.util.StrUtil;
import com.agileboot.domain.common.cache.CacheCenter;
import com.agileboot.domain.social.credential.db.SocialCredentialEntity;
import com.agileboot.domain.social.credential.db.SocialCredentialService;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * B站cookie会话管理：DB持久化（social_credential）+ Redis登录会话。
 * <p>
 * cookie串存储格式："k1=v1; k2=v2"，merge时LinkedHashMap保序覆盖。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiliCookieStore {

    public static final String PLATFORM_BILI = "bili";

    private static final String SESSDATA = "SESSDATA";

    private final SocialCredentialService credentialService;

    /**
     * 请求前拼Cookie头：DB凭据 + Redis登录会话的buvid3/buvid4（扫码期间尚未登录，buvid以会话为准）
     */
    public String buildCookieHeader(Long accountId) {
        Map<String, String> cookies = new LinkedHashMap<>();
        SocialCredentialEntity credential = credentialService.getByAccountId(accountId);
        if (credential != null && StrUtil.isNotBlank(credential.getCookie())
            && Integer.valueOf(1).equals(credential.getStatus())) {
            cookies.putAll(parseCookieString(credential.getCookie()));
        }
        BiliLoginSession session = loadLoginSession(accountId);
        if (session != null) {
            if (StrUtil.isNotBlank(session.getBuvid3())) {
                cookies.put("buvid3", session.getBuvid3());
            }
            if (StrUtil.isNotBlank(session.getBuvid4())) {
                cookies.put("buvid4", session.getBuvid4());
            }
        }
        return joinCookies(cookies);
    }

    /**
     * 响应回调：解析Set-Cookie列表merge进该账号cookie串并upsert；
     * 提取SESSDATA的Expires写expires_at
     */
    public void onResponseCookies(Long accountId, List<String> setCookies) {
        Map<String, String> parsed = new LinkedHashMap<>();
        Date sessdataExpires = null;
        for (String header : setCookies) {
            String kv = StrUtil.subBefore(header, ';', false);
            String name = StrUtil.subBefore(kv, '=', false).trim();
            String value = StrUtil.subAfter(kv, '=', false).trim();
            if (StrUtil.isBlank(name)) {
                continue;
            }
            parsed.put(name, value);
            if (SESSDATA.equals(name)) {
                sessdataExpires = parseExpires(header);
            }
        }
        if (parsed.isEmpty()) {
            return;
        }

        SocialCredentialEntity credential = credentialService.getByAccountId(accountId);
        Map<String, String> merged = new LinkedHashMap<>();
        if (credential != null && StrUtil.isNotBlank(credential.getCookie())) {
            merged.putAll(parseCookieString(credential.getCookie()));
        }
        parsed.forEach((name, value) -> {
            // 空value是删除型cookie
            if (StrUtil.isEmpty(value)) {
                merged.remove(name);
            } else {
                merged.put(name, value);
            }
        });

        if (credential == null) {
            credential = new SocialCredentialEntity();
            credential.setAccountId(accountId);
            credential.setPlatform(PLATFORM_BILI);
            credential.setStatus(1);
        }
        credential.setCookie(joinCookies(merged));
        if (sessdataExpires != null) {
            credential.setExpiresAt(sessdataExpires);
        }
        credentialService.saveOrUpdate(credential);
        log.info("B站账号 {} cookie已更新（{}个键）", accountId, merged.size());
    }

    /**
     * 扫码登录成功：refresh_token落库，记录登录时间，置有效
     */
    public void onLoginSuccess(Long accountId, String refreshToken) {
        SocialCredentialEntity credential = credentialService.getByAccountId(accountId);
        if (credential == null) {
            credential = new SocialCredentialEntity();
            credential.setAccountId(accountId);
            credential.setPlatform(PLATFORM_BILI);
        }
        credential.setRefreshToken(refreshToken);
        credential.setLastLoginTime(new Date());
        credential.setStatus(1);
        credentialService.saveOrUpdate(credential);
        clearLoginSession(accountId);
    }

    /**
     * nav判定cookie失效时调用：置失效，保留记录便于排查
     */
    public void markInvalid(Long accountId) {
        SocialCredentialEntity credential = credentialService.getByAccountId(accountId);
        if (credential != null && !Integer.valueOf(0).equals(credential.getStatus())) {
            credential.setStatus(0);
            credentialService.updateById(credential);
            log.info("B站账号 {} cookie已失效", accountId);
        }
    }

    /**
     * 是否有有效凭据（DB层面，不代表cookie真的没过服务器侧校验）
     */
    public boolean hasValidCredential(Long accountId) {
        SocialCredentialEntity credential = credentialService.getByAccountId(accountId);
        return credential != null && Integer.valueOf(1).equals(credential.getStatus())
            && StrUtil.isNotBlank(credential.getCookie());
    }

    public void saveLoginSession(Long accountId, String qrcodeKey, String buvid3, String buvid4) {
        CacheCenter.biliLoginSessionCache.set(accountId, new BiliLoginSession(qrcodeKey, buvid3, buvid4));
    }

    public BiliLoginSession loadLoginSession(Long accountId) {
        return CacheCenter.biliLoginSessionCache.getObjectOnlyInCacheById(accountId);
    }

    public void clearLoginSession(Long accountId) {
        CacheCenter.biliLoginSessionCache.delete(accountId);
    }

    /**
     * 解析cookie串为保序map（后者覆盖前者）
     */
    public static Map<String, String> parseCookieString(String cookieString) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StrUtil.isBlank(cookieString)) {
            return map;
        }
        for (String pair : cookieString.split(";")) {
            // 无=号的片段不是合法cookie对，跳过
            if (!pair.contains("=")) {
                continue;
            }
            String name = StrUtil.subBefore(pair, '=', false).trim();
            String value = StrUtil.subAfter(pair, '=', false).trim();
            if (StrUtil.isNotBlank(name)) {
                map.put(name, value);
            }
        }
        return map;
    }

    private static String joinCookies(Map<String, String> cookies) {
        return cookies.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("; "));
    }

    /**
     * 解析Set-Cookie头的Expires属性（RFC_1123格式，如 "Wed, 21 Oct 2026 07:28:00 GMT"）
     */
    private static Date parseExpires(String setCookieHeader) {
        for (String attr : setCookieHeader.split(";")) {
            String trimmed = attr.trim();
            if (trimmed.regionMatches(true, 0, "Expires=", 0, 8)) {
                String value = trimmed.substring(8).trim();
                try {
                    return Date.from(ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                } catch (Exception e) {
                    log.debug("解析cookie Expires失败: {}", value);
                    return null;
                }
            }
        }
        return null;
    }

}
