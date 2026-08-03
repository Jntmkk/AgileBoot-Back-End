package com.agileboot.domain.social.post;

import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.social.post.db.SocialSyncPostEntity;
import com.agileboot.domain.social.post.db.SocialSyncPostService;
import com.agileboot.domain.social.post.dto.SocialSyncPostDTO;
import com.agileboot.domain.social.post.query.SocialSyncPostQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 动态同步记录应用服务
 *
 * @author SocialMedia-Hub
 */
@Service
@RequiredArgsConstructor
public class SocialSyncPostApplicationService {

    /**
     * 音频状态：1 待转写（本机 ASR worker 消费）
     */
    public static final int AUDIO_STATUS_READY = 1;
    /**
     * 音频状态：5 待总结（转写完成，等待 AI 总结）
     */
    public static final int AUDIO_STATUS_TRANSCRIBED = 5;

    private final SocialSyncPostService postService;

    public PageDTO<SocialSyncPostDTO> getPostList(SocialSyncPostQuery query) {
        Page<SocialSyncPostEntity> page = postService.page(query.toPage(), query.toQueryWrapper());
        List<SocialSyncPostDTO> records = page.getRecords().stream()
            .map(SocialSyncPostDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public SocialSyncPostDTO getPostInfo(Long id) {
        SocialSyncPostEntity entity = postService.getById(id);
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + id);
        }
        return new SocialSyncPostDTO(entity);
    }

    /**
     * 手动重触发转写：把 audio_status 重置为 1，本机 ASR worker 会重新消费。
     * 仅对视频动态（post_type=2）有意义。
     */
    public void retriggerTranscribe(Long id) {
        SocialSyncPostEntity entity = postService.getById(id);
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + id);
        }
        if (entity.getPostType() == null || entity.getPostType() != 2) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "仅视频动态可触发转写");
        }
        entity.setAudioStatus(AUDIO_STATUS_READY);
        entity.setAudioTranscript(null);
        entity.setAudioSummary(null);
        entity.setRemark("手动重触发转写");
        entity.updateById();
    }

    /**
     * 手动重触发总结：把 audio_status 置为 5（已有 transcript），等 AI 总结阶段消费。
     * 要求 transcript 已存在。
     */
    public void retriggerSummary(Long id) {
        SocialSyncPostEntity entity = postService.getById(id);
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + id);
        }
        if (entity.getAudioTranscript() == null || entity.getAudioTranscript().isEmpty()) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "尚无转写文本，无法总结");
        }
        entity.setAudioStatus(AUDIO_STATUS_TRANSCRIBED);
        entity.setAudioSummary(null);
        entity.setRemark("手动重触发总结");
        entity.updateById();
    }

}
