package com.agileboot.domain.social.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 社交媒体接入配置
 *
 * @author SocialMedia-Hub
 */
@Data
@Component
@ConfigurationProperties(prefix = "social")
public class SocialMediaProperties {

    /**
     * 账号容器访问基址（不含端口）。
     * 本地开发：http://host.docker.internal
     * 生产环境：http://frpc-visitor（经 stcp 隧道到住宅节点）
     */
    private String baseUrl = "http://host.docker.internal";

    /**
     * 端口基数：账号容器端口 = portBase + 账号ID
     */
    private int portBase = 18060;

    /**
     * 节点心跳令牌（X-Node-Token）
     */
    private String nodeToken = "changeme";

    /**
     * n8n 工作流接入配置（动态同步触发）
     */
    private N8n n8n = new N8n();

    /**
     * 调用账号容器的 HTTP 超时（毫秒）。
     * 登录状态/二维码涉及浏览器导航，耗时较长。
     */
    private int httpTimeoutMs = 90000;

    /**
     * B站API配置（直连官方web API，不经过住宅节点，baseUrl/portBase对bili不生效）
     */
    private Bilibili bilibili = new Bilibili();

    /**
     * 云盘配置（alist 代理）
     */
    private CloudDrive cloudDrive = new CloudDrive();

    @Data
    public static class N8n {

        /**
         * n8n webhook 完整地址（含路径），如 https://n8n2.frxxz.top/webhook/bili-sync
         */
        private String webhookUrl = "";

        /**
         * webhook 鉴权令牌（X-Sync-Token 头），与 n8n webhook 节点校验一致
         */
        private String syncToken = "";

        /**
         * 调用 n8n webhook 的超时（毫秒）。webhook 只入队不等待全量执行，宜短
         */
        private int timeoutMs = 15000;
    }

    @Data
    public static class Bilibili {

        /**
         * 桌面Chrome UA（风控校验请求头完整性）
         */
        private String ua =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)"
                + " Chrome/126.0.0.0 Safari/537.36";

        private String referer = "https://www.bilibili.com";

        private int connectTimeoutMs = 10000;

        /**
         * B站API都是轻量JSON，不需要xhs容器那种90s
         */
        private int readTimeoutMs = 15000;

        /**
         * 二维码有效期（秒），B站实际180s
         */
        private int qrcodeTimeoutSeconds = 180;

        /**
         * 动态同步专用的 B站登录 Cookie（含 SESSDATA）。
         * 从环境变量 BILI_SYNC_COOKIE 注入，不提交到仓库。
         */
        private String syncCookie = "";

        /**
         * 单次拉取动态条数（polymer 端点）。
         */
        private int syncPageSize = 50;
    }

    @Data
    public static class CloudDrive {

        /**
         * alist API 地址（云端后端通过 frp 访问本机 alist）。
         */
        private String alistUrl = "http://localhost:5244";

        /**
         * 本机 alist 地址（worker 在 localhost 直接用，生成 download URL）。
         * 为空时使用 alistUrl。
         */
        private String localAlistUrl = "http://localhost:5244";

        /**
         * alist 管理员 token（通过 /api/auth/login 获取）。
         * 用于后端 API 调用认证。
         */
        private String alistToken = "";

        /**
         * 调用 alist API 的超时（毫秒）。
         */
        private int timeoutMs = 15000;

        /**
         * 阿里云盘 Open API 的 ClientID（从 alistgo 工具获取）。
         */
        private String aliyundriveClientId = "";

        /**
         * 阿里云盘 Open API 的 ClientSecret（从 alistgo 工具获取）。
         */
        private String aliyundriveClientSecret = "";
    }

}
