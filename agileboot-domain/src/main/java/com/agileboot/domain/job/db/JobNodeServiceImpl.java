package com.agileboot.domain.job.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class JobNodeServiceImpl extends ServiceImpl<JobNodeMapper, JobNodeEntity>
    implements JobNodeService {

    @Override
    public JobNodeEntity getByNodeId(String nodeId) {
        if (nodeId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<JobNodeEntity>()
            .eq(JobNodeEntity::getNodeId, nodeId), false);
    }

}
