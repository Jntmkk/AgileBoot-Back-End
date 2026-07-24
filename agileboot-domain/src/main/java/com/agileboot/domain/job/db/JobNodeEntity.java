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
@TableName("job_node")
@ApiModel(value = "JobNodeEntity对象", description = "任务执行节点注册表")
public class JobNodeEntity extends BaseEntity<JobNodeEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("节点表主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("节点唯一标识")
    @TableField("node_id")
    private String nodeId;

    @ApiModelProperty("节点可读名称")
    @TableField("node_name")
    private String nodeName;

    @ApiModelProperty("节点大类（worker/residential/local_dev）")
    @TableField("node_type")
    private String nodeType;

    @ApiModelProperty("能力标签数组（JSON）")
    @TableField("capabilities")
    private String capabilities;

    @ApiModelProperty("自定义标签（JSON）")
    @TableField("labels")
    private String labels;

    @ApiModelProperty("通信协议（poll/push/hybrid）")
    @TableField("protocol")
    private String protocol;

    @ApiModelProperty("推送地址（protocol=push时必填）")
    @TableField("endpoint")
    private String endpoint;

    @ApiModelProperty("节点认证令牌")
    @TableField("token")
    private String token;

    @ApiModelProperty("最大并发任务数")
    @TableField("max_concurrent")
    private Integer maxConcurrent;

    @ApiModelProperty("当前正在执行的任务数")
    @TableField("current_load")
    private Integer currentLoad;

    @ApiModelProperty("Worker版本号")
    @TableField("version")
    private String version;

    @ApiModelProperty("节点出口IP")
    @TableField("ip_address")
    private String ipAddress;

    @ApiModelProperty("状态（ONLINE/OFFLINE/DISABLED/BUSY）")
    @TableField("`status`")
    private String status;

    @ApiModelProperty("最后心跳时间")
    @TableField("last_heartbeat")
    private Date lastHeartbeat;

    @ApiModelProperty("预期心跳间隔（秒）")
    @TableField("heartbeat_interval_seconds")
    private Integer heartbeatIntervalSeconds;

    @ApiModelProperty("离线判定阈值（秒）")
    @TableField("offline_threshold_seconds")
    private Integer offlineThresholdSeconds;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
