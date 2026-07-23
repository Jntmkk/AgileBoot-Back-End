package com.agileboot.domain.social.credential;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author SocialMedia-Hub
 */
class BiliCookieStoreParseTest {

    @Test
    void should_parse_cookie_string_in_order() {
        Map<String, String> map = BiliCookieStore.parseCookieString("SESSDATA=abc; bili_jct=xyz; buvid3=b3");

        assertEquals(3, map.size());
        assertEquals("abc", map.get("SESSDATA"));
        assertEquals("xyz", map.get("bili_jct"));
        assertEquals("b3", map.get("buvid3"));
    }

    @Test
    void should_handle_blank_and_malformed_pairs() {
        assertTrue(BiliCookieStore.parseCookieString(null).isEmpty());
        assertTrue(BiliCookieStore.parseCookieString("").isEmpty());
        assertTrue(BiliCookieStore.parseCookieString("  ").isEmpty());

        // 无=号的片段被忽略，空value保留为空串
        Map<String, String> map = BiliCookieStore.parseCookieString("a=1; broken; b=2");
        assertEquals(2, map.size());
        assertNull(map.get("broken"));
    }

    @Test
    void should_keep_value_containing_equals() {
        Map<String, String> map = BiliCookieStore.parseCookieString("SESSDATA=a=b=c");
        assertEquals("a=b=c", map.get("SESSDATA"));
    }

    @Test
    void later_pair_overrides_earlier() {
        Map<String, String> map = BiliCookieStore.parseCookieString("a=1; b=2; a=3");
        assertEquals("3", map.get("a"));
        assertEquals(2, map.size());
    }

}
