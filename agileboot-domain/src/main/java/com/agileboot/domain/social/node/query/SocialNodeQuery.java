package com.agileboot.domain.social.node.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.social.node.db.SocialNodeEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SocialNodeQuery extends AbstractPageQuery<SocialNodeEntity> {

    private String nodeName;

    @Override
    public QueryWrapper<SocialNodeEntity> addQueryCondition() {
        return new QueryWrapper<SocialNodeEntity>()
            .like(StrUtil.isNotEmpty(nodeName), "node_name", nodeName)
            .orderByAsc("id");
    }
}
