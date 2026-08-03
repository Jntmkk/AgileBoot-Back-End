package com.agileboot.domain.social.post.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.social.post.db.SocialSyncPostEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 动态同步记录查询
 *
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SocialSyncPostQuery extends AbstractPageQuery<SocialSyncPostEntity> {

    private String title;

    private String nickname;

    private String platform;

    private Integer postType;

    private Integer audioStatus;

    @Override
    public QueryWrapper<SocialSyncPostEntity> addQueryCondition() {
        return new QueryWrapper<SocialSyncPostEntity>()
            .like(StrUtil.isNotEmpty(title), "title", title)
            .like(StrUtil.isNotEmpty(nickname), "nickname", nickname)
            .eq(StrUtil.isNotEmpty(platform), "platform", platform)
            .eq(postType != null, "post_type", postType)
            .eq(audioStatus != null, "audio_status", audioStatus)
            .orderByDesc("published_at")
            .orderByDesc("id");
    }
}
