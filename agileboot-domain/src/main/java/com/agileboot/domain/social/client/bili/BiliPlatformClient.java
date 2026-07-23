package com.agileboot.domain.social.client.bili;

import cn.hutool.core.codec.Base64;
import cn.hutool.extra.qrcode.QrCodeUtil;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.client.SocialPlatformClient;
import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.NavData;
import com.agileboot.domain.social.client.bili.dto.QrcodeGenerateData;
import com.agileboot.domain.social.client.bili.dto.QrcodePollData;
import com.agileboot.domain.social.client.bili.dto.SpiData;
import com.agileboot.domain.social.client.dto.SocialLoginStatus;
import com.agileboot.domain.social.client.dto.SocialQrcode;
import com.agileboot.domain.social.client.dto.SocialUserProfile;
import com.agileboot.domain.social.config.SocialMediaProperties;
import com.agileboot.domain.social.credential.BiliCookieStore;
import com.agileboot.domain.social.credential.BiliLoginSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * B站平台客户端：扫码登录/状态/我的信息全流程编排。
 * <p>
 * 直连B站官方web API，cookie由 {@link BiliCookieStore} 持久化到DB。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiliPlatformClient implements SocialPlatformClient {

    private final BiliApiClient biliApiClient;

    private final BiliCookieStore cookieStore;

    private final SocialMediaProperties properties;

    @Override
    public String platform() {
        return BiliCookieStore.PLATFORM_BILI;
    }

    @Override
    public SocialQrcode getLoginQrcode(SocialAccountEntity account) {
        Long accountId = account.getId();
        // 已有有效cookie则直接返回已登录（对齐xhs行为）
        if (cookieStore.hasValidCredential(accountId) && isCookieAlive(accountId)) {
            return SocialQrcode.alreadyLoggedIn();
        }
        // 1. 先取buvid（风控要求扫码全程携带buvid）
        SpiData spi = biliApiClient.spi(accountId);
        // 2. 申请二维码
        QrcodeGenerateData qrcode = biliApiClient.generateQrcode(accountId);
        // 3. 登录会话存Redis（qrcode_key + buvid），有效期=二维码有效期
        cookieStore.saveLoginSession(accountId, qrcode.getQrcodeKey(), spi.getBuvid3(), spi.getBuvid4());
        // 4. B站返回的是二维码内容文本，后端统一渲染为base64图片（前端协议与xhs一致）
        String img = Base64.encode(QrCodeUtil.generatePng(qrcode.getUrl(), 240, 240));
        return new SocialQrcode(img, properties.getBilibili().getQrcodeTimeoutSeconds(), false);
    }

    @Override
    public SocialLoginStatus checkLoginStatus(SocialAccountEntity account) {
        Long accountId = account.getId();
        SocialLoginStatus status = new SocialLoginStatus();
        BiliLoginSession session = cookieStore.loadLoginSession(accountId);
        if (session != null) {
            return pollQrcodeStatus(accountId, session, status);
        }
        // 无登录会话：校验存量cookie
        boolean alive = isCookieAlive(accountId);
        status.setIsLoggedIn(alive);
        return status;
    }

    @Override
    public SocialUserProfile getMyProfile(SocialAccountEntity account) {
        try {
            BiliResp<NavData> resp = biliApiClient.nav(account.getId());
            NavData data = resp.getData();
            if (data == null || !Boolean.TRUE.equals(data.getIsLogin())) {
                return null;
            }
            return new SocialUserProfile(
                data.getMid() == null ? null : String.valueOf(data.getMid()),
                data.getUname(), data.getFace());
        } catch (Exception e) {
            log.warn("获取B站账号 {} 用户信息失败: {}", account.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 二维码扫描中：轮询四态分支
     */
    private SocialLoginStatus pollQrcodeStatus(Long accountId, BiliLoginSession session,
        SocialLoginStatus status) {
        QrcodePollData poll = biliApiClient.pollQrcode(accountId, session.getQrcodeKey());
        status.setIsLoggedIn(false);
        switch (poll.getCode() == null ? -1 : poll.getCode()) {
            case QrcodePollData.CODE_SUCCESS:
                // Set-Cookie已被拦截器落库，这里补refresh_token并清会话
                cookieStore.onLoginSuccess(accountId, poll.getRefreshToken());
                status.setIsLoggedIn(true);
                break;
            case QrcodePollData.CODE_SCANNED_PENDING:
                status.setQrStatus("已扫码，请在手机上确认");
                break;
            case QrcodePollData.CODE_EXPIRED:
                cookieStore.clearLoginSession(accountId);
                status.setQrStatus("二维码已过期，请重新获取");
                break;
            case QrcodePollData.CODE_NOT_SCANNED:
            default:
                status.setQrStatus("请使用哔哩哔哩客户端扫码");
                break;
        }
        return status;
    }

    /**
     * 调nav校验cookie服务器侧有效性；失效则标记
     */
    private boolean isCookieAlive(Long accountId) {
        try {
            BiliResp<NavData> resp = biliApiClient.nav(accountId);
            boolean alive = resp.success() && resp.getData() != null
                && Boolean.TRUE.equals(resp.getData().getIsLogin());
            if (!alive) {
                cookieStore.markInvalid(accountId);
            }
            return alive;
        } catch (Exception e) {
            log.warn("校验B站账号 {} cookie有效性失败: {}", accountId, e.getMessage());
            // 网络/风控等异常时按DB状态返回，避免误判失效
            return cookieStore.hasValidCredential(accountId);
        }
    }

}
