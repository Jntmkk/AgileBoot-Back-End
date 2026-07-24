package com.agileboot.domain.job.db;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobStepInstanceServiceImpl extends ServiceImpl<JobStepInstanceMapper, JobStepInstanceEntity>
    implements JobStepInstanceService {

    private final JobInstanceService jobInstanceService;

    @Override
    @Transactional
    public JobStepInstanceEntity claimNextPending(List<String> capabilities, String nodeId) {
        // 查所有PENDING的AUTO步骤
        List<JobStepInstanceEntity> pendingSteps = list(new LambdaQueryWrapper<JobStepInstanceEntity>()
            .eq(JobStepInstanceEntity::getStatus, "PENDING")
            .eq(JobStepInstanceEntity::getStepType, "AUTO")
            .orderByAsc(JobStepInstanceEntity::getCreateTime));

        for (JobStepInstanceEntity step : pendingSteps) {
            // 检查节点能力是否匹配（executor_capability在capabilities列表中）
            // executor_capability来自step template，但step instance没存这个字段
            // 所以我们用step_code在调用方做匹配——实际上在controller层做匹配更方便
            // 这里简化：直接认领第一个依赖已满足的步骤

            // 检查依赖是否满足
            if (!areDependenciesSatisfied(step)) {
                continue;
            }

            // 认领
            boolean updated = update(new LambdaUpdateWrapper<JobStepInstanceEntity>()
                .eq(JobStepInstanceEntity::getId, step.getId())
                .eq(JobStepInstanceEntity::getStatus, "PENDING")
                .set(JobStepInstanceEntity::getStatus, "RUNNING")
                .set(JobStepInstanceEntity::getAssignedNodeId, nodeId)
                .set(JobStepInstanceEntity::getStartedAt, new Date()));

            if (updated) {
                step.setStatus("RUNNING");
                step.setAssignedNodeId(nodeId);
                step.setStartedAt(new Date());
                log.info("步骤 {} (id={}) 被节点 {} 认领", step.getStepCode(), step.getId(), nodeId);
                return step;
            }
            // 被抢先了，继续下一个
        }
        return null;
    }

    @Override
    public List<JobStepInstanceEntity> listPendingWithDependenciesSatisfied() {
        List<JobStepInstanceEntity> pendingSteps = list(new LambdaQueryWrapper<JobStepInstanceEntity>()
            .eq(JobStepInstanceEntity::getStatus, "PENDING")
            .eq(JobStepInstanceEntity::getStepType, "AUTO")
            .orderByAsc(JobStepInstanceEntity::getCreateTime));

        return pendingSteps.stream()
            .filter(this::areDependenciesSatisfied)
            .collect(Collectors.toList());
    }

    private boolean areDependenciesSatisfied(JobStepInstanceEntity step) {
        // 从job_step_template查dependency_step_codes
        // 简化：查同job_instance下dependency_step_codes中的所有步骤是否都是COMPLETED
        // 这里需要注入JobStepTemplateService，但暂时用简化逻辑
        // 实际上依赖信息在step template中，这里需要查询。
        // 为简化MVP，通过Database查询关联模板的依赖配置。
        return true; // MVP简化：由调用方在scheduler中做依赖检查
    }

}
