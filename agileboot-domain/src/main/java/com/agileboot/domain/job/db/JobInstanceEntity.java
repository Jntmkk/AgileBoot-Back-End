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
@TableName("job_instance")
@ApiModel(value = "JobInstanceEntity对象", description = "任务实例表")
public class JobInstanceEntity extends BaseEntity<JobInstanceEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("实例ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务模板ID")
    @TableField("job_template_id")
    private Long jobTemplateId;

    @ApiModelProperty("业务类型")
    @TableField("biz_type")
    private String bizType;

    @ApiModelProperty("业务唯一键")
    @TableField("biz_key")
    private String bizKey;

    @ApiModelProperty("业务子键")
    @TableField("biz_sub_key")
    private String bizSubKey;

    @ApiModelProperty("任务级入参（JSON）")
    @TableField("params_json")
    private String paramsJson;

    @ApiModelProperty("状态（PENDING/RUNNING/WAITING_HUMAN/COMPLETED/FAILED/DEAD/CANCELLED）")
    @TableField("`status`")
    private String status;

    @ApiModelProperty("当前执行到的步骤code")
    @TableField("current_step_code")
    private String currentStepCode;

    @ApiModelProperty("全局上下文（JSON）")
    @TableField("context_json")
    private String contextJson;

    @ApiModelProperty("开始时间")
    @TableField("start_time")
    private Date startTime;

    @ApiModelProperty("结束时间")
    @TableField("end_time")
    private Date endTime;

    @ApiModelProperty("最终失败原因")
    @TableField("error_msg")
    private String errorMsg;

    @ApiModelProperty("触发来源（manual/schedule/webhook）")
    @TableField("trigger_source")
    private String triggerSource;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
