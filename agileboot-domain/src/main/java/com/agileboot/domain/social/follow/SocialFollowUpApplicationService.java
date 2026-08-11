package com.agileboot.domain.social.follow;

import cn.hutool.core.bean.BeanUtil;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.clouddrive.AlistProxyService;
import com.agileboot.domain.social.config.SocialMediaProperties;
import com.agileboot.domain.social.follow.command.BackfillCommand;
import com.agileboot.domain.social.follow.command.SocialFollowUpAddCommand;
import com.agileboot.domain.social.follow.command.SocialFollowUpUpdateCommand;
import com.agileboot.domain.social.follow.command.SyncByLinkCommand;
import com.agileboot.domain.social.follow.db.SocialFollowUpEntity;
import com.agileboot.domain.social.follow.db.SocialFollowUpService;
import com.agileboot.domain.social.follow.dto.SocialFollowUpDTO;
import com.agileboot.domain.social.follow.query.SocialFollowUpQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 关注UP主应用服务
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialFollowUpApplicationService {

    private final SocialFollowUpService followService;
    private final BiliSyncService biliSyncService;
    private final SocialMediaProperties socialMediaProperties;
    private final AlistProxyService alistProxyService;

    public PageDTO<SocialFollowUpDTO> getFollowList(SocialFollowUpQuery query) {
        Page<SocialFollowUpEntity> page = followService.page(query.toPage(), query.toQueryWrapper());
        List<SocialFollowUpDTO> records = page.getRecords().stream()
            .map(SocialFollowUpDTO::new)
            .collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public SocialFollowUpDTO getFollowInfo(Long id) {
        SocialFollowUpEntity entity = followService.getById(id);
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + id);
        }
        return new SocialFollowUpDTO(entity);
    }

    public void addFollow(SocialFollowUpAddCommand command) {
        if (exists(command.getPlatform(), command.getUpId())) {
            throw new ApiException(ErrorCode.FAILED, "该 UP 已存在: " + command.getUpId());
        }
        SocialFollowUpEntity entity = new SocialFollowUpEntity();
        BeanUtil.copyProperties(command, entity);
        entity.setStatus(defaultValue(entity.getStatus(), 1));
        entity.setSyncEnabled(defaultValue(entity.getSyncEnabled(), 1));
        entity.insert();

        if ("aliyun".equals(command.getPlatform())
            && command.getRemark() != null
            && command.getUpAvatar() != null) {
            try {
                alistProxyService.createAliyundriveStorage(
                    command.getRemark(), command.getUpAvatar());
            } catch (Exception e) {
                log.error("auto-create alist storage failed: mount_path={}, err={}",
                    command.getRemark(), e.getMessage());
            }
        }
    }

    public void updateFollow(SocialFollowUpUpdateCommand command) {
        SocialFollowUpEntity entity = followService.getById(command.getId());
        if (entity == null) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "记录不存在: " + command.getId());
        }
        BeanUtil.copyProperties(command, entity);
        entity.setStatus(defaultValue(entity.getStatus(), 1));
        entity.setSyncEnabled(defaultValue(entity.getSyncEnabled(), 1));
        entity.updateById();
    }

    public void deleteFollow(BulkOperationCommand<Long> command) {
        for (Long id : command.getIds()) {
            SocialFollowUpEntity entity = followService.getById(id);
            if (entity != null && "aliyun".equals(entity.getPlatform())
                && entity.getRemark() != null) {
                try {
                    alistProxyService.removeStorage(entity.getRemark());
                } catch (Exception e) {
                    log.error("auto-remove alist storage failed: mount_path={}, err={}",
                        entity.getRemark(), e.getMessage());
                }
            }
        }
        followService.removeBatchByIds(command.getIds());
    }

    /**
     * 同步指定链接：后端直接解析并写入 social_sync_post。
     */
    public void syncByLink(SyncByLinkCommand command) {
        validateLink(command);
        biliSyncService.syncByLink(command);
    }

    /**
     * 按时间范围补数据：后端直接拉取历史并写入 social_sync_post。
     */
    public void backfill(BackfillCommand command) {
        biliSyncService.backfill(command);
    }

    private void validateLink(SyncByLinkCommand command) {
        if ("bili".equals(command.getPlatform()) && !command.getUrl().contains("bilibili.com")) {
            throw new ApiException(ErrorCode.Client.COMMON_REQUEST_PARAMETERS_INVALID, "仅支持 bilibili.com 链接");
        }
    }

    private boolean exists(String platform, String upId) {
        return followService.lambdaQuery()
            .eq(SocialFollowUpEntity::getPlatform, platform)
            .eq(SocialFollowUpEntity::getUpId, upId)
            .eq(SocialFollowUpEntity::getDeleted, false)
            .count() > 0;
    }

    private Integer defaultValue(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

}
