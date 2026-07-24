package com.agileboot.domain.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agileboot.job")
public class JobOrchestrationProperties {

    /** 产物文件存储根目录 */
    private String artifactBaseDir = "/data/artifacts";

    /** 调度器轮询间隔（毫秒） */
    private long schedulerPollIntervalMs = 5000;

}
