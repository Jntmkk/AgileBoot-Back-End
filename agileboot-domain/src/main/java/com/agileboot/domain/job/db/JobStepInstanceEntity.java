package com.agileboot.domain.job.db;

import com.agileboot.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_step_instance")
@ApiModel(value = "JobStepInstanceEntity对象", description = "任务步骤实例表")
public class JobStepInstanceEntity extends BaseEntity<JobStepInstanceEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("步骤实例ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务实例ID")
    @TableField("job_instance_id")
    private Long jobInstanceId;

    @ApiModelProperty("步骤模板ID")
    @TableField("job_step_template_id")
    private Long jobStepTemplateId;

    @ApiModelProperty("步骤编码")
    @TableField("step_code")
    private String stepCode;

    @ApiModelProperty("步骤名称")
    @TableField("step_name")
    private String stepName;

    @ApiModelProperty("步骤类型（AUTO/HUMAN/GATEWAY）")
    @TableField("step_type")
    private String stepType;

    @ApiModelProperty("状态（PENDING/RUNNING/WAITING_HUMAN/COMPLETED/FAILED/DEAD/TIMEOUT）")
    @TableField("`status`")
    private String status;

    @ApiModelProperty("分配到的节点ID")
    @TableField("assigned_node_id")
    private String assignedNodeId;

    @ApiModelProperty("实际执行节点类型")
    @TableField("node_type")
    private String nodeType;

    @ApiModelProperty("输入参数（JSON）")
    @TableField("input_json")
    private String inputJson;

    @ApiModelProperty("输出结果（JSON）")
    @TableField("output_json")
    private String outputJson;

    @ApiModelProperty("输入产物ID数组（JSON）")
    @TableField("input_artifact_ids")
    private String inputArtifactIds;

    @ApiModelProperty("输出产物ID数组（JSON）")
    @TableField("output_artifact_ids")
    private String outputArtifactIds;

    @ApiModelProperty("开始执行时间")
    @TableField("started_at")
    private Date startedAt;

    @ApiModelProperty("结束时间")
    @TableField("ended_at")
    private Date endedAt;

    @ApiModelProperty("超时截止时间")
    @TableField("timeout_at")
    private Date timeoutAt;

    @ApiModelProperty("已重试次数")
    @TableField("retry_count")
    private Integer retryCount;

    @ApiModelProperty("失败原因")
    @TableField("error_msg")
    private String errorMsg;

    @ApiModelProperty("前序步骤实例ID")
    @TableField("previous_step_id")
    private Long previousStepId;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
