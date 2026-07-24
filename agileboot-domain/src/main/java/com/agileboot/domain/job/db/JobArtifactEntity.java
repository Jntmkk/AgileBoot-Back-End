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
@TableName("job_artifact")
@ApiModel(value = "JobArtifactEntity对象", description = "任务产物表")
public class JobArtifactEntity extends BaseEntity<JobArtifactEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("产物ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("任务实例ID")
    @TableField("job_instance_id")
    private Long jobInstanceId;

    @ApiModelProperty("产生该产物的步骤实例ID")
    @TableField("job_step_instance_id")
    private Long jobStepInstanceId;

    @ApiModelProperty("产物类型（raw_json/audio/transcript/summary/log/image）")
    @TableField("artifact_type")
    private String artifactType;

    @ApiModelProperty("存储类型（db/local/oss/minio）")
    @TableField("storage_type")
    private String storageType;

    @ApiModelProperty("文本内容（storage_type=db时使用）")
    @TableField("content")
    private String content;

    @ApiModelProperty("文件路径或URL")
    @TableField("file_path")
    private String filePath;

    @ApiModelProperty("文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    @ApiModelProperty("文件哈希（sha256）")
    @TableField("file_hash")
    private String fileHash;

    @ApiModelProperty("状态（1有效 0已清理）")
    @TableField("`status`")
    private Integer status;

    @ApiModelProperty("过期清理时间")
    @TableField("expired_at")
    private Date expiredAt;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
