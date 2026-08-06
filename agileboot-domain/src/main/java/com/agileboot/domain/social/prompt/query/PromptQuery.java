package com.agileboot.domain.social.prompt.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.social.prompt.db.SocialSummaryPromptEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PromptQuery extends AbstractPageQuery<SocialSummaryPromptEntity> {

    private String upId;

    private String keyword;

    @Override
    public QueryWrapper<SocialSummaryPromptEntity> addQueryCondition() {
        return new QueryWrapper<SocialSummaryPromptEntity>()
            .eq(StrUtil.isNotEmpty(upId), "up_id", upId)
            .like(StrUtil.isNotEmpty(keyword), "keyword", keyword)
            .orderByAsc("up_id", "sort_order");
    }

}
