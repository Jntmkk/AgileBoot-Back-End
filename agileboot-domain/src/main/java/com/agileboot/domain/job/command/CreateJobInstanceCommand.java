package com.agileboot.domain.job.command;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateJobInstanceCommand {

    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @NotBlank(message = "业务键不能为空")
    private String bizKey;

    /** 任务参数JSON，如 {"mid": 456664012} */
    private String paramsJson;

    /** 业务子键（可选，如分P编号、重跑批次） */
    private String bizSubKey;

}
