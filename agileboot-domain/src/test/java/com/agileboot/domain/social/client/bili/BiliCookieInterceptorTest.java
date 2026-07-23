package com.agileboot.domain.social.client.bili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agileboot.domain.social.credential.BiliCookieStore;
import java.io.IOException;
import java.util.Collections;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author SocialMedia-Hub
 */
class BiliCookieInterceptorTest {

    private final BiliCookieStore cookieStore = mock(BiliCookieStore.class);

    private final BiliCookieInterceptor interceptor = new BiliCookieInterceptor(cookieStore);

    @Test
    void should_inject_cookie_and_strip_account_header() throws IOException {
        when(cookieStore.buildCookieHeader(42L)).thenReturn("SESSDATA=abc; bili_jct=xyz");
        Request request = new Request.Builder()
            .url("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=k")
            .header(BiliPassportApi.HEADER_ACCOUNT_ID, "42")
            .build();
        Interceptor.Chain chain = mockChain(request, responseBuilder(request).build());

        interceptor.intercept(chain);

        // 出网请求：账号头被移除，Cookie头被注入
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(chain).proceed(captor.capture());
        Request outgoing = captor.getValue();
        assertNull(outgoing.header(BiliPassportApi.HEADER_ACCOUNT_ID));
        assertEquals("SESSDATA=abc; bili_jct=xyz", outgoing.header("Cookie"));
    }

    @Test
    void should_capture_set_cookie_to_store() throws IOException {
        when(cookieStore.buildCookieHeader(42L)).thenReturn("");
        Request request = new Request.Builder()
            .url("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=k")
            .header(BiliPassportApi.HEADER_ACCOUNT_ID, "42")
            .build();
        Response response = responseBuilder(request)
            .addHeader("Set-Cookie", "SESSDATA=v1; Path=/; Expires=Wed, 21 Oct 2026 07:28:00 GMT")
            .addHeader("Set-Cookie", "bili_jct=v2; Path=/")
            .build();
        Interceptor.Chain chain = mockChain(request, response);

        interceptor.intercept(chain);

        verify(cookieStore).onResponseCookies(eq(42L), anyList());
    }

    @Test
    void should_skip_store_when_no_account_header() throws IOException {
        Request request = new Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .build();
        Interceptor.Chain chain = mockChain(request, responseBuilder(request).build());

        interceptor.intercept(chain);

        verify(cookieStore, never()).buildCookieHeader(any());
        verify(cookieStore, never()).onResponseCookies(any(), anyList());
    }

    @Test
    void should_not_touch_store_when_no_set_cookie() throws IOException {
        when(cookieStore.buildCookieHeader(7L)).thenReturn("a=1");
        Request request = new Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .header(BiliPassportApi.HEADER_ACCOUNT_ID, "7")
            .build();
        Interceptor.Chain chain = mockChain(request, responseBuilder(request).build());

        interceptor.intercept(chain);

        verify(cookieStore, never()).onResponseCookies(eq(7L), anyList());
    }

    private Interceptor.Chain mockChain(Request request, Response response) throws IOException {
        Interceptor.Chain chain = mock(Interceptor.Chain.class);
        when(chain.request()).thenReturn(request);
        when(chain.proceed(any(Request.class))).thenReturn(response);
        return chain;
    }

    private Response.Builder responseBuilder(Request request) {
        return new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(MediaType.parse("application/json"), "{}"));
    }

}
