package com.agileboot.domain.social.post.db;

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
 * 社交动态同步记录表（B站UP主动态，由 n8n 工作流 + 本机 ASR worker 写入）
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Getter
@Setter
@TableName("social_sync_post")
@ApiModel(value = "SocialSyncPostEntity对象", description = "社交动态同步记录表")
public class SocialSyncPostEntity extends BaseEntity<SocialSyncPostEntity> {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("平台（bili 预留 douyin）")
    @TableField("platform")
    private String platform;

    @ApiModelProperty("平台用户ID")
    @TableField("platform_user_id")
    private String platformUserId;

    @ApiModelProperty("作者昵称")
    @TableField("nickname")
    private String nickname;

    @ApiModelProperty("平台动态ID")
    @TableField("platform_post_id")
    private String platformPostId;

    @ApiModelProperty("动态类型（1图文 2视频）")
    @TableField("post_type")
    private Integer postType;

    @ApiModelProperty("标题")
    @TableField("title")
    private String title;

    @ApiModelProperty("正文/描述")
    @TableField("content")
    private String content;

    @ApiModelProperty("视频地址")
    @TableField("video_url")
    private String videoUrl;

    @ApiModelProperty("封面地址")
    @TableField("cover_url")
    private String coverUrl;

    @ApiModelProperty("图片列表（JSON数组）")
    @TableField("images")
    private String images;

    @ApiModelProperty("平台动态链接")
    @TableField("platform_post_url")
    private String platformPostUrl;

    @ApiModelProperty("发布时间")
    @TableField("published_at")
    private Date publishedAt;

    @ApiModelProperty("同步时间")
    @TableField("synced_at")
    private Date syncedAt;

    @ApiModelProperty("状态")
    @TableField("`status`")
    private Integer status;

    @ApiModelProperty("阅读数")
    @TableField("read_count")
    private Integer readCount;

    @ApiModelProperty("点赞数")
    @TableField("like_count")
    private Integer likeCount;

    @ApiModelProperty("评论数")
    @TableField("comment_count")
    private Integer commentCount;

    @ApiModelProperty("分享数")
    @TableField("share_count")
    private Integer shareCount;

    @ApiModelProperty("投币数")
    @TableField("coin_count")
    private Integer coinCount;

    @ApiModelProperty("原始元数据（JSON）")
    @TableField("raw_meta")
    private String rawMeta;

    @ApiModelProperty("备注")
    @TableField("remark")
    private String remark;

    @ApiModelProperty("音频地址")
    @TableField("audio_url")
    private String audioUrl;

    @ApiModelProperty("音频转写文本（ASR）")
    @TableField("audio_transcript")
    private String audioTranscript;

    @ApiModelProperty("句子级转写时间戳（JSON数组）")
    @TableField("audio_transcript_sentences")
    private String audioTranscriptSentences;

    @ApiModelProperty("AI 内容总结")
    @TableField("audio_summary")
    private String audioSummary;

    @ApiModelProperty("音频状态（0新 1待转写 2取址失败 3已总结 4转写中 5待总结 6转写失败）")
    @TableField("audio_status")
    private Integer audioStatus;

    @ApiModelProperty("音频下载时间")
    @TableField("audio_downloaded_at")
    private Date audioDownloadedAt;

    @ApiModelProperty("AI 总结时间")
    @TableField("audio_summarized_at")
    private Date audioSummarizedAt;

    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
