package com.agileboot.domain.job.query;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.page.AbstractPageQuery;
import com.agileboot.domain.job.db.JobInstanceEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class JobInstanceQuery extends AbstractPageQuery<JobInstanceEntity> {

    private String bizKey;

    private String status;

    public JobInstanceQuery() {
        this.orderColumn = "create_time";
        this.orderDirection = "descending";
    }

    @Override
    public QueryWrapper<JobInstanceEntity> addQueryCondition() {
        QueryWrapper<JobInstanceEntity> wrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(bizKey)) {
            wrapper.eq("biz_key", bizKey);
        }
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq("status", status);
        }
        return wrapper;
    }

}
