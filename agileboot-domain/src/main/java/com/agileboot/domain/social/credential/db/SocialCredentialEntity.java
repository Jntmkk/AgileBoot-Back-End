package com.agileboot.domain.social.credential.db;

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
 * 社交平台登录凭据表（cookie会话DB持久化，与账号一对一）
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Getter
@Setter
@TableName("social_credential")
@ApiModel(value = "SocialCredentialEntity对象", description = "社交平台登录凭据表")
public class SocialCredentialEntity extends BaseEntity<SocialCredentialEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("凭据ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("social_account.id")
    @TableField("account_id")
    private Long accountId;

    @ApiModelProperty("平台（bili）")
    @TableField("platform")
    private String platform;

    @ApiModelProperty("完整Cookie串（SESSDATA/bili_jct/DedeUserID/buvid3...）")
    @TableField("cookie")
    private String cookie;

    @ApiModelProperty("B站刷新令牌（cookie刷新流程预留）")
    @TableField("refresh_token")
    private String refreshToken;

    @ApiModelProperty("凭据过期时间（取自SESSDATA的Expires，未知可空）")
    @TableField("expires_at")
    private Date expiresAt;

    @ApiModelProperty("最近一次扫码登录成功时间")
    @TableField("last_login_time")
    private Date lastLoginTime;

    @ApiModelProperty("凭据状态（1有效 0失效）")
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
