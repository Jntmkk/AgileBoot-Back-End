package com.agileboot.domain.social.follow.dto;

import com.agileboot.domain.social.follow.db.SocialFollowUpEntity;
import java.util.Date;
import lombok.Data;

/**
 * 关注UP主 DTO
 *
 * @author SocialMedia-Hub
 */
@Data
public class SocialFollowUpDTO {

    public SocialFollowUpDTO(SocialFollowUpEntity entity) {
        if (entity == null) {
            return;
        }
        this.id = entity.getId() + "";
        this.platform = entity.getPlatform();
        this.upId = entity.getUpId();
        this.upName = entity.getUpName();
        this.upAvatar = entity.getUpAvatar();
        this.status = entity.getStatus();
        this.syncEnabled = entity.getSyncEnabled();
        this.lastSyncAt = entity.getLastSyncAt();
        this.remark = entity.getRemark();
        this.createTime = entity.getCreateTime();
    }

    private String id;

    private String platform;

    private String upId;

    private String upName;

    private String upAvatar;

    private Integer status;

    private Integer syncEnabled;

    private Date lastSyncAt;

    private String remark;

    private Date createTime;

}
