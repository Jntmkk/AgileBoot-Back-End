package com.agileboot.domain.social.node.db;

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

/**
 * <p>
 * 住宅节点表（节点 agent 心跳自动 upsert）
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Getter
@Setter
@TableName("social_node")
@ApiModel(value = "SocialNodeEntity对象", description = "住宅节点表")
public class SocialNodeEntity extends BaseEntity<SocialNodeEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("节点ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("节点标识")
    @TableField("node_name")
    private String nodeName;

    @ApiModelProperty("住宅出口IP")
    @TableField("egress_ip")
    private String egressIp;

    @ApiModelProperty("IP类型（residential住宅 pool代理池）")
    @TableField("ip_type")
    private String ipType;

    @ApiModelProperty("最后心跳时间")
    @TableField("last_heartbeat")
    private Date lastHeartbeat;

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
