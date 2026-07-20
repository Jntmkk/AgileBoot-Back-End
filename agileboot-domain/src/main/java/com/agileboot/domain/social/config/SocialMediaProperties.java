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
     * 调用账号容器的 HTTP 超时（毫秒）。
     * 登录状态/二维码涉及浏览器导航，耗时较长。
     */
    private int httpTimeoutMs = 90000;

}
