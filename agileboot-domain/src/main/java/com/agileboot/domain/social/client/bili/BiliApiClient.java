package com.agileboot.domain.social.client.bili;

import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Internal;
import com.agileboot.domain.social.client.bili.dto.BiliResp;
import com.agileboot.domain.social.client.bili.dto.BiliSpaceVideoListData;
import com.agileboot.domain.social.client.bili.dto.NavData;
import com.agileboot.domain.social.client.bili.dto.QrcodeGenerateData;
import com.agileboot.domain.social.client.bili.dto.QrcodePollData;
import com.agileboot.domain.social.client.bili.dto.SpiData;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

/**
 * B站API高级客户端：统一解包 {@link BiliResp}，异常风格对齐 XhsApiClient。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BiliApiClient {

    /**
     * B站业务code：未登录
     */
    public static final int CODE_NOT_LOGIN = -101;

    private final BiliPassportApi passportApi;

    private final BiliWebApi webApi;

    private final BiliWbiSigner wbiSigner;

    public QrcodeGenerateData generateQrcode(Long accountId) {
        return unwrap(accountId, passportApi.generateQrcode(accountId), "qrcode/generate");
    }

    /**
     * 轮询扫码状态。注意不走统一unwrap：86101/86090/86038是HTTP 200的业务正常态，
     * 由调用方按 {@link QrcodePollData#getCode()} 分支处理
     */
    public QrcodePollData pollQrcode(Long accountId, String qrcodeKey) {
        BiliResp<QrcodePollData> body = execute(accountId, passportApi.pollQrcode(accountId, qrcodeKey),
            "qrcode/poll");
        if (body == null || !body.success() || body.getData() == null) {
            String message = body == null ? "空响应" : body.getMessage();
            throw new ApiException(Internal.INTERNAL_ERROR, "B站扫码状态查询失败: " + message);
        }
        return body.getData();
    }

    /**
     * 导航栏用户信息。code=-101（未登录/cookie失效）时正常返回body，由调用方判断；
     * 其他失败抛异常
     */
    public BiliResp<NavData> nav(Long accountId) {
        BiliResp<NavData> body = execute(accountId, webApi.nav(accountId), "nav");
        if (body == null) {
            throw new ApiException(Internal.INTERNAL_ERROR, "B站用户信息接口空响应");
        }
        if (!body.success() && !Integer.valueOf(CODE_NOT_LOGIN).equals(body.getCode())) {
            throw apiError("nav", body.getCode(), body.getMessage());
        }
        return body;
    }

    public SpiData spi(Long accountId) {
        return unwrap(accountId, webApi.spi(accountId), "finger/spi");
    }

    /**
     * 获取WBI签名密钥（img_key + sub_key）。
     */
    /**
     * 搜索UP主空间投稿（需WBI签名）。
     *
     * @param accountId B站账号ID（0表示无需登录）
     * @param mid UP主mid
     * @param ps 每页条数（最大30）
     * @param pn 页码
     */
    public BiliSpaceVideoListData searchSpace(Long accountId, Long mid, int ps, int pn) {
        Map<String, String> params = new HashMap<>();
        params.put("mid", String.valueOf(mid));
        params.put("ps", String.valueOf(ps));
        params.put("pn", String.valueOf(pn));
        params.put("order", "pubdate");
        params.put("tid", "0");
        params.put("keyword", "");
        wbiSigner.sign(params);
        return unwrap(accountId, webApi.searchSpace(accountId, params), "space/wbi/arc/search");
    }

    private <T> T unwrap(Long accountId, Call<BiliResp<T>> call, String apiName) {
        BiliResp<T> body = execute(accountId, call, apiName);
        if (body == null || !body.success() || body.getData() == null) {
            Integer code = body == null ? null : body.getCode();
            String message = body == null ? "空响应" : body.getMessage();
            throw apiError(apiName, code, message);
        }
        return body.getData();
    }

    private <T> BiliResp<T> execute(Long accountId, Call<BiliResp<T>> call, String apiName) {
        try {
            Response<BiliResp<T>> response = call.execute();
            if (!response.isSuccessful()) {
                log.warn("B站API HTTP错误 accountId={} api={} httpCode={}", accountId, apiName, response.code());
                throw new ApiException(Internal.INTERNAL_ERROR,
                    "B站接口HTTP错误(" + response.code() + "): " + apiName);
            }
            return response.body();
        } catch (IOException e) {
            log.warn("调用B站API失败 accountId={} api={}: {}", accountId, apiName, e.getMessage());
            throw new ApiException(Internal.INTERNAL_ERROR, "B站接口调用失败: " + e.getMessage());
        }
    }

    private ApiException apiError(String apiName, Integer code, String message) {
        // -352风控校验失败给明确文案
        if (Integer.valueOf(-352).equals(code)) {
            return new ApiException(Internal.INTERNAL_ERROR,
                "触发B站风控校验(-352)，请稍后重试: " + apiName);
        }
        return new ApiException(Internal.INTERNAL_ERROR,
            "B站接口错误(" + code + ") " + apiName + ": " + message);
    }

}
