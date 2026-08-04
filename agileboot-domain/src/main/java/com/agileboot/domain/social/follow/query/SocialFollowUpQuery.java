package com.agileboot.domain.social.follow.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.social.follow.db.SocialFollowUpEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 关注UP主查询
 *
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SocialFollowUpQuery extends AbstractPageQuery<SocialFollowUpEntity> {

    private String platform;

    private String upId;

    private String upName;

    private Integer status;

    private Integer syncEnabled;

    @Override
    public QueryWrapper<SocialFollowUpEntity> addQueryCondition() {
        return new QueryWrapper<SocialFollowUpEntity>()
            .like(StrUtil.isNotEmpty(upId), "up_id", upId)
            .like(StrUtil.isNotEmpty(upName), "up_name", upName)
            .eq(StrUtil.isNotEmpty(platform), "platform", platform)
            .eq(status != null, "status", status)
            .eq(syncEnabled != null, "sync_enabled", syncEnabled)
            .orderByDesc("last_sync_at")
            .orderByAsc("id");
    }

}
