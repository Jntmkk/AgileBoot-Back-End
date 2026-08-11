package com.agileboot.domain.social.post.dto;

import com.agileboot.domain.social.post.db.SocialSyncPostEntity;
import java.util.Date;
import lombok.Data;

/**
 * 动态同步记录 DTO（列表 + 详情共用，长文本字段在详情接口才返回）
 *
 * @author SocialMedia-Hub
 */
@Data
public class SocialSyncPostDTO {

    public SocialSyncPostDTO(SocialSyncPostEntity e) {
        if (e == null) {
            return;
        }
        this.id = e.getId() + "";
        this.platform = e.getPlatform();
        this.platformUserId = e.getPlatformUserId();
        this.nickname = e.getNickname();
        this.platformPostId = e.getPlatformPostId();
        this.postType = e.getPostType();
        this.title = e.getTitle();
        this.content = e.getContent();
        this.videoUrl = e.getVideoUrl();
        this.coverUrl = e.getCoverUrl();
        this.images = e.getImages();
        this.platformPostUrl = e.getPlatformPostUrl();
        this.publishedAt = e.getPublishedAt();
        this.syncedAt = e.getSyncedAt();
        this.createTime = e.getCreateTime();
        this.likeCount = e.getLikeCount();
        this.commentCount = e.getCommentCount();
        this.shareCount = e.getShareCount();
        this.coinCount = e.getCoinCount();
        this.audioStatus = e.getAudioStatus();
        this.audioDownloadedAt = e.getAudioDownloadedAt();
        this.audioSummarizedAt = e.getAudioSummarizedAt();
        this.remark = e.getRemark();
        // 长文本字段
        this.audioTranscript = e.getAudioTranscript();
        this.audioSentenceTimestamps = e.getAudioTranscriptSentences();
        this.audioSummary = e.getAudioSummary();
        this.audioUrl = e.getAudioUrl();
    }

    private String id;

    private String platform;

    private String platformUserId;

    private String nickname;

    private String platformPostId;

    private Integer postType;

    private String title;

    private String content;

    private String videoUrl;

    private String coverUrl;

    private String images;

    private String platformPostUrl;

    private Date publishedAt;

    private Date syncedAt;

    private Date createTime;

    private Integer likeCount;

    private Integer commentCount;

    private Integer shareCount;

    private Integer coinCount;

    private Integer audioStatus;

    private Date audioDownloadedAt;

    private Date audioSummarizedAt;

    private String remark;

    private String audioUrl;

    private String audioTranscript;

    private String audioSentenceTimestamps;

    private String audioSummary;

}
