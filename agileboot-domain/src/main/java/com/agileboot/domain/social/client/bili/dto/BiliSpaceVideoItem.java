package com.agileboot.domain.social.client.bili.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * B站空间投稿视频条目。
 *
 * @author SocialMedia-Hub
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiliSpaceVideoItem {

    /** BV号 */
    private String bvid;

    /** 标题 */
    private String title;

    /** 封面图URL */
    private String pic;

    /** 发布时间戳（秒） */
    private Long pubdate;

    /** 时长，格式 "mm:ss" */
    private String length;

    /** 描述 */
    private String description;

    /** 播放数 */
    private Long play;

    /** 弹幕数 */
    @JsonProperty("video_review")
    private Long videoReview;

    /** 评论数 */
    private Long comment;

    /** AV号 */
    private Long aid;

    /** 分P数 */
    private Long videos;

    /** 分区ID */
    private Long tid;

    /** 分区名 */
    @JsonProperty("tname")
    private String tname;

}
