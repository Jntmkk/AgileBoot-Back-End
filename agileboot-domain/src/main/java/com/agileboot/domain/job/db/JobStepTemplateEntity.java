package com.agileboot.domain.job.db;

import com.agileboot.common.core.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("job_step_template")
@ApiModel(value = "JobStepTemplateEntity对象", description = "任务步骤模板表")
public class JobStepTemplateEntity extends BaseEntity<JobStepTemplateEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("步骤模板ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("所属任务模板ID")
    @TableField("job_template_id")
    private Long jobTemplateId;

    @ApiModelProperty("步骤编码")
    @TableField("step_code")
    private String stepCode;

    @ApiModelProperty("步骤名称")
    @TableField("step_name")
    private String stepName;

    @ApiModelProperty("步骤类型（AUTO/HUMAN/GATEWAY）")
    @TableField("step_type")
    private String stepType;

    @ApiModelProperty("排序")
    @TableField("order_index")
    private Integer orderIndex;

    @ApiModelProperty("执行器能力标签")
    @TableField("executor_capability")
    private String executorCapability;

    @ApiModelProperty("步骤超时时间（秒）")
    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    @ApiModelProperty("失败重试次数")
    @TableField("retry_times")
    private Integer retryTimes;

    @ApiModelProperty("重试间隔（秒）")
    @TableField("retry_interval_seconds")
    private Integer retryIntervalSeconds;

    @ApiModelProperty("前置步骤code数组（JSON）")
    @TableField("dependency_step_codes")
    private String dependencyStepCodes;

    @ApiModelProperty("输入参数JSON Schema")
    @TableField("input_schema")
    private String inputSchema;

    @ApiModelProperty("输出参数JSON Schema")
    @TableField("output_schema")
    private String outputSchema;

    @ApiModelProperty("节点选择策略（JSON）")
    @TableField("node_selector")
    private String nodeSelector;

    @ApiModelProperty("描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("状态（1启用 0停用）")
    @TableField("`status`")
    private Integer status;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
