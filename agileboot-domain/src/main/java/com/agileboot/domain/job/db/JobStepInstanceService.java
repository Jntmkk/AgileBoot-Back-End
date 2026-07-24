package com.agileboot.domain.job.db;

import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface JobStepInstanceService extends IService<JobStepInstanceEntity> {

    /**
     * 原子认领一个匹配能力的PENDING步骤（用于Worker轮询）。
     * 查status=PENDING且executor_capability在capabilities列表中且依赖已满足的步骤，
     * 匹配后更新status=RUNNING、assigned_node_id、started_at。
     *
     * @param capabilities 节点能力列表
     * @param nodeId 认领节点ID
     * @return 认领到的步骤实例，无匹配返回null
     */
    JobStepInstanceEntity claimNextPending(List<String> capabilities, String nodeId);

    /**
     * 查询所有依赖已满足的PENDING步骤（用于调度器扫描）。
     */
    List<JobStepInstanceEntity> listPendingWithDependenciesSatisfied();

}
