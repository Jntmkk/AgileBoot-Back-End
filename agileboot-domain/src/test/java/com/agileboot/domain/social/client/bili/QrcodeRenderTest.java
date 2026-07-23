package com.agileboot.domain.social.client.bili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.hutool.core.codec.Base64;
import cn.hutool.extra.qrcode.QrCodeUtil;
import org.junit.jupiter.api.Test;

/**
 * B站二维码url文本渲染为base64 PNG（前端与xhs协议一致）。
 *
 * @author SocialMedia-Hub
 */
class QrcodeRenderTest {

    @Test
    void should_render_bili_qrcode_url_to_base64_png() {
        byte[] png = QrCodeUtil.generatePng("https://passport.bilibili.com/h5-app/passport/login/scan?qrcode_key=abc",
            240, 240);

        // PNG magic bytes: 89 50 4E 47
        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);

        // base64编码后可解码回原始字节
        String base64 = Base64.encode(png);
        assertEquals(png.length, Base64.decode(base64).length);
    }

}
