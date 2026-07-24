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
@TableName("job_template")
@ApiModel(value = "JobTemplateEntity对象", description = "任务模板表")
public class JobTemplateEntity extends BaseEntity<JobTemplateEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("模板ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("模板编码")
    @TableField("template_code")
    private String templateCode;

    @ApiModelProperty("模板名称")
    @TableField("template_name")
    private String templateName;

    @ApiModelProperty("业务类型")
    @TableField("biz_type")
    private String bizType;

    @ApiModelProperty("描述")
    @TableField("description")
    private String description;

    @ApiModelProperty("状态（1启用 0停用）")
    @TableField("`status`")
    private Integer status;

    @ApiModelProperty("版本号")
    @TableField("version")
    private Integer version;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
