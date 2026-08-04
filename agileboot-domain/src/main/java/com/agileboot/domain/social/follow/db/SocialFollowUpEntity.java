package com.agileboot.domain.social.follow.db;

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
 * 关注UP主列表（动态同步抓取目标，按平台可扩展）
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Getter
@Setter
@TableName("social_follow_up")
@ApiModel(value = "SocialFollowUpEntity对象", description = "关注UP主列表")
public class SocialFollowUpEntity extends BaseEntity<SocialFollowUpEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("平台（bili哔哩 xhs小红书 douyin抖音）")
    @TableField("platform")
    private String platform;

    @ApiModelProperty("UP主平台ID（B站mid/小红书号/抖音号）")
    @TableField("up_id")
    private String upId;

    @ApiModelProperty("UP主昵称（冗余展示）")
    @TableField("up_name")
    private String upName;

    @ApiModelProperty("头像链接")
    @TableField("up_avatar")
    private String upAvatar;

    @ApiModelProperty("状态（1启用 0停用）")
    @TableField("`status`")
    private Integer status;

    @ApiModelProperty("是否参与自动同步（1是 0否）")
    @TableField("sync_enabled")
    private Integer syncEnabled;

    @ApiModelProperty("最近同步时间")
    @TableField("last_sync_at")
    private Date lastSyncAt;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
