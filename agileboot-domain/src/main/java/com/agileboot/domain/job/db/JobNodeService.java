package com.agileboot.domain.job.db;

import com.baomidou.mybatisplus.extension.service.IService;

public interface JobNodeService extends IService<JobNodeEntity> {

    /**
     * 按node_id查找节点
     */
    JobNodeEntity getByNodeId(String nodeId);

}
