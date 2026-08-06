package com.agileboot.domain.social.prompt.db;

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
@TableName("social_summary_prompt")
@ApiModel(value = "SocialSummaryPromptEntity对象", description = "AI总结提示词配置")
public class SocialSummaryPromptEntity extends BaseEntity<SocialSummaryPromptEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("UP主平台ID（* 表示兜底默认）")
    @TableField("up_id")
    private String upId;

    @ApiModelProperty("标题匹配关键词")
    @TableField("keyword")
    private String keyword;

    @ApiModelProperty("系统提示词")
    @TableField("system_prompt")
    private String systemPrompt;

    @ApiModelProperty("排序（越小越优先）")
    @TableField("sort_order")
    private Integer sortOrder;

    @ApiModelProperty("状态（1启用 0停用）")
    @TableField("`status`")
    private Integer status;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
