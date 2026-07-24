package com.agileboot.domain.job.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class JobInstanceDTO {

    private Long id;

    private Long jobTemplateId;

    private String templateCode;

    private String templateName;

    private String bizType;

    private String bizKey;

    private String bizSubKey;

    private String paramsJson;

    private String status;

    private String currentStepCode;

    private String contextJson;

    private Date startTime;

    private Date endTime;

    private String errorMsg;

    private String triggerSource;

    private Date createTime;

    private List<JobStepInstanceDTO> steps;

}
