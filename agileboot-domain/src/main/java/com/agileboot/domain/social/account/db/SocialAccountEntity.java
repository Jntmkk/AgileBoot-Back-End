package com.agileboot.domain.social.account.db;

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

/**
 * <p>
 * 社交媒体账号表
 * 端口规则：容器端口 = 18060 + id，stcp 隧道名 = social-acc-{id}
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Getter
@Setter
@TableName("social_account")
@ApiModel(value = "SocialAccountEntity对象", description = "社交媒体账号表")
public class SocialAccountEntity extends BaseEntity<SocialAccountEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("账号ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("平台（xhs小红书 预留douyin）")
    @TableField("platform")
    private String platform;

    @ApiModelProperty("账号备注名")
    @TableField("account_name")
    private String accountName;

    @ApiModelProperty("平台侧用户ID（登录后回写，小红书号/B站mid）")
    @TableField("platform_user_id")
    private String platformUserId;

    @ApiModelProperty("所在住宅节点名（运维参考）")
    @TableField("node_name")
    private String nodeName;

    @ApiModelProperty("代理地址（IP池预留）")
    @TableField("proxy_url")
    private String proxyUrl;

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
