package com.agileboot.domain.job.dto;

import java.util.Date;
import lombok.Data;

@Data
public class JobStepInstanceDTO {

    private Long id;

    private Long jobInstanceId;

    private String stepCode;

    private String stepName;

    private String stepType;

    private String status;

    private String assignedNodeId;

    private String inputJson;

    private String outputJson;

    private String inputArtifactIds;

    private String outputArtifactIds;

    private Date startedAt;

    private Date endedAt;

    private Integer retryCount;

    private String errorMsg;

    private Long previousStepId;

}
