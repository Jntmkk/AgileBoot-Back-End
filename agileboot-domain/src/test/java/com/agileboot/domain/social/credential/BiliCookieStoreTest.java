package com.agileboot.domain.social.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agileboot.domain.common.cache.CacheCenter;
import com.agileboot.domain.social.credential.db.SocialCredentialEntity;
import com.agileboot.domain.social.credential.db.SocialCredentialService;
import com.agileboot.infrastructure.cache.redis.RedisCacheTemplate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author SocialMedia-Hub
 */
class BiliCookieStoreTest {

    private SocialCredentialService credentialService;
    private BiliCookieStore cookieStore;
    private RedisCacheTemplate<BiliLoginSession> sessionCache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        credentialService = mock(SocialCredentialService.class);
        cookieStore = new BiliCookieStore(credentialService);
        sessionCache = mock(RedisCacheTemplate.class);
        CacheCenter.biliLoginSessionCache = sessionCache;
    }

    @AfterEach
    void tearDown() {
        CacheCenter.biliLoginSessionCache = null;
    }

    @Test
    void should_build_cookie_header_from_db_credential() {
        SocialCredentialEntity credential = credential("a=1; b=2", 1);
        when(credentialService.getByAccountId(1L)).thenReturn(credential);
        when(sessionCache.getObjectOnlyInCacheById(1L)).thenReturn(null);

        assertEquals("a=1; b=2", cookieStore.buildCookieHeader(1L));
    }

    @Test
    void should_merge_session_buvid_over_db_cookie() {
        SocialCredentialEntity credential = credential("SESSDATA=old; buvid3=db-b3", 1);
        when(credentialService.getByAccountId(1L)).thenReturn(credential);
        when(sessionCache.getObjectOnlyInCacheById(1L))
            .thenReturn(new BiliLoginSession("qr-key", "session-b3", "session-b4"));

        String header = cookieStore.buildCookieHeader(1L);

        assertEquals("SESSDATA=old; buvid3=session-b3; buvid4=session-b4", header);
    }

    @Test
    void should_skip_invalid_credential() {
        when(credentialService.getByAccountId(1L)).thenReturn(credential("a=1", 0));
        when(sessionCache.getObjectOnlyInCacheById(1L)).thenReturn(null);

        assertEquals("", cookieStore.buildCookieHeader(1L));
    }

    @Test
    void should_merge_set_cookies_into_existing_credential() {
        SocialCredentialEntity credential = credential("SESSDATA=old; bili_jct=j1", 1);
        when(credentialService.getByAccountId(1L)).thenReturn(credential);

        List<String> setCookies = Arrays.asList(
            "SESSDATA=new-value; Path=/; Expires=Wed, 21 Oct 2026 07:28:00 GMT; HttpOnly",
            "DedeUserID=12345; Path=/");
        cookieStore.onResponseCookies(1L, setCookies);

        ArgumentCaptor<SocialCredentialEntity> captor = ArgumentCaptor.forClass(SocialCredentialEntity.class);
        verify(credentialService).saveOrUpdate(captor.capture());
        SocialCredentialEntity saved = captor.getValue();
        assertEquals("SESSDATA=new-value; bili_jct=j1; DedeUserID=12345", saved.getCookie());
        // SESSDATA的Expires已解析落库
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getExpiresAt());
    }

    @Test
    void should_create_credential_when_absent() {
        when(credentialService.getByAccountId(2L)).thenReturn(null);

        cookieStore.onResponseCookies(2L,
            Collections.singletonList("SESSDATA=v; Path=/; Expires=Wed, 21 Oct 2026 07:28:00 GMT"));

        ArgumentCaptor<SocialCredentialEntity> captor = ArgumentCaptor.forClass(SocialCredentialEntity.class);
        verify(credentialService).saveOrUpdate(captor.capture());
        assertEquals(2L, captor.getValue().getAccountId());
        assertEquals(BiliCookieStore.PLATFORM_BILI, captor.getValue().getPlatform());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void should_remove_cookie_with_empty_value() {
        SocialCredentialEntity credential = credential("SESSDATA=v; buvid3=b3", 1);
        when(credentialService.getByAccountId(1L)).thenReturn(credential);

        cookieStore.onResponseCookies(1L, Collections.singletonList("buvid3=; Path=/; Max-Age=0"));

        ArgumentCaptor<SocialCredentialEntity> captor = ArgumentCaptor.forClass(SocialCredentialEntity.class);
        verify(credentialService).saveOrUpdate(captor.capture());
        assertFalse(captor.getValue().getCookie().contains("buvid3"));
    }

    @Test
    void should_mark_invalid_only_when_credential_exists() {
        SocialCredentialEntity credential = credential("a=1", 1);
        when(credentialService.getByAccountId(1L)).thenReturn(credential);

        cookieStore.markInvalid(1L);

        assertEquals(0, credential.getStatus());
    }

    @Test
    void mark_invalid_should_noop_when_absent() {
        when(credentialService.getByAccountId(9L)).thenReturn(null);
        // 不抛异常即可
        cookieStore.markInvalid(9L);
        verify(credentialService, org.mockito.Mockito.never()).saveOrUpdate(any());
    }

    @Test
    void should_report_valid_credential() {
        when(credentialService.getByAccountId(1L)).thenReturn(credential("a=1", 1));
        when(credentialService.getByAccountId(2L)).thenReturn(credential("a=1", 0));
        when(credentialService.getByAccountId(3L)).thenReturn(null);

        assertTrue(cookieStore.hasValidCredential(1L));
        assertFalse(cookieStore.hasValidCredential(2L));
        assertFalse(cookieStore.hasValidCredential(3L));
    }

    private SocialCredentialEntity credential(String cookie, int status) {
        SocialCredentialEntity entity = new SocialCredentialEntity();
        entity.setAccountId(1L);
        entity.setPlatform(BiliCookieStore.PLATFORM_BILI);
        entity.setCookie(cookie);
        entity.setStatus(status);
        return entity;
    }

}
