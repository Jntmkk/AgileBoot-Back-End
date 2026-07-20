package com.agileboot.domain.social.account.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
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
public class SocialAccountQuery extends AbstractPageQuery<SocialAccountEntity> {

    private String platform;

    private String accountName;

    private String nodeName;

    @Override
    public QueryWrapper<SocialAccountEntity> addQueryCondition() {
        return new QueryWrapper<SocialAccountEntity>()
            .like(StrUtil.isNotEmpty(accountName), "account_name", accountName)
            .eq(StrUtil.isNotEmpty(platform), "platform", platform)
            .eq(StrUtil.isNotEmpty(nodeName), "node_name", nodeName)
            .orderByAsc("id");
    }
}
