package com.agileboot.domain.job.schedule;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.domain.job.db.JobInstanceEntity;
import com.agileboot.domain.job.db.JobInstanceService;
import com.agileboot.domain.job.db.JobStepInstanceEntity;
import com.agileboot.domain.job.db.JobStepInstanceService;
import com.agileboot.domain.job.db.JobStepTemplateEntity;
import com.agileboot.domain.job.db.JobStepTemplateService;
import com.agileboot.domain.job.executor.StepExecutor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 任务步骤调度器：定期扫描PENDING步骤，依赖满足的java能力步骤在进程内执行。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobStepInstanceService stepInstanceService;

    private final JobStepTemplateService stepTemplateService;

    private final JobInstanceService jobInstanceService;

    private final List<StepExecutor> executors;

    /**
     * 每5秒扫描一次PENDING步骤。
     */
    @Scheduled(fixedDelay = 5000)
    public void scan() {
        try {
            List<JobStepInstanceEntity> pendingSteps = stepInstanceService.list(
                new LambdaQueryWrapper<JobStepInstanceEntity>()
                    .eq(JobStepInstanceEntity::getStatus, "PENDING")
                    .eq(JobStepInstanceEntity::getStepType, "AUTO")
                    .orderByAsc(JobStepInstanceEntity::getCreateTime));

            for (JobStepInstanceEntity step : pendingSteps) {
                if (!areDependenciesSatisfied(step)) {
                    continue;
                }
                // 查步骤模板获取executor_capability
                JobStepTemplateEntity template = stepTemplateService.getById(step.getJobStepTemplateId());
                if (template == null) {
                    log.warn("步骤实例 {} 找不到模板 {}", step.getId(), step.getJobStepTemplateId());
                    continue;
                }
                String capability = template.getExecutorCapability();
                // 只处理java能力的步骤（在本地执行）
                if (!"java".equals(capability)) {
                    continue;
                }
                // 找到匹配的执行器
                StepExecutor executor = findExecutor(capability);
                if (executor == null) {
                    log.debug("未找到 capability={} 的执行器，跳过 stepId={}", capability, step.getId());
                    continue;
                }
                // 原子认领：更新状态为RUNNING
                boolean claimed = stepInstanceService.update(
                    new LambdaUpdateWrapper<JobStepInstanceEntity>()
                        .eq(JobStepInstanceEntity::getId, step.getId())
                        .eq(JobStepInstanceEntity::getStatus, "PENDING")
                        .set(JobStepInstanceEntity::getStatus, "RUNNING")
                        .set(JobStepInstanceEntity::getStartedAt, new Date()));
                if (!claimed) {
                    continue; // 被抢先了
                }
                step.setStatus("RUNNING");
                step.setStartedAt(new Date());

                // 加载任务实例
                JobInstanceEntity job = jobInstanceService.getById(step.getJobInstanceId());
                if (job == null) {
                    step.setStatus("FAILED");
                    step.setErrorMsg("找不到所属任务实例");
                    step.setEndedAt(new Date());
                    stepInstanceService.updateById(step);
                    continue;
                }
                if (!"PENDING".equals(job.getStatus()) && !"RUNNING".equals(job.getStatus())) {
                    continue; // 任务已结束
                }
                // 首次执行时更新任务状态
                if ("PENDING".equals(job.getStatus())) {
                    job.setStatus("RUNNING");
                    job.setStartTime(new Date());
                }

                // 执行
                log.info("调度器开始执行步骤: jobId={}, stepId={}, stepCode={}",
                    job.getId(), step.getId(), step.getStepCode());
                executor.execute(step, job);

                // 保存步骤和任务状态
                stepInstanceService.updateById(step);
                jobInstanceService.updateById(job);

                // 处理_execNextStep：如果executor在context中标记了下一步，创建它
                createNextStepFromContext(job, step);
            }
        } catch (Exception e) {
            log.error("调度器扫描异常", e);
        }
    }

    private StepExecutor findExecutor(String capability) {
        return executors.stream()
            .filter(e -> e.capability().equals(capability))
            .findFirst()
            .orElse(null);
    }

    /**
     * 检查步骤的所有前置依赖是否已完成。
     */
    private boolean areDependenciesSatisfied(JobStepInstanceEntity step) {
        JobStepTemplateEntity template = stepTemplateService.getById(step.getJobStepTemplateId());
        if (template == null || template.getDependencyStepCodes() == null) {
            return true;
        }
        JSONArray deps;
        try {
            deps = JSONUtil.parseArray(template.getDependencyStepCodes());
        } catch (Exception e) {
            return true;
        }
        if (deps.isEmpty()) {
            return true;
        }
        // 查同job_instance下已完成的步骤
        List<JobStepInstanceEntity> completedSteps = stepInstanceService.list(
            new LambdaQueryWrapper<JobStepInstanceEntity>()
                .eq(JobStepInstanceEntity::getJobInstanceId, step.getJobInstanceId())
                .eq(JobStepInstanceEntity::getStatus, "COMPLETED"));
        List<String> completedCodes = completedSteps.stream()
            .map(JobStepInstanceEntity::getStepCode)
            .collect(Collectors.toList());
        for (Object dep : deps) {
            if (!completedCodes.contains(dep.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 从job.context_json中读取_execNextStep并创建下一步骤实例。
     */
    private void createNextStepFromContext(JobInstanceEntity job, JobStepInstanceEntity currentStep) {
        if (job.getContextJson() == null) {
            return;
        }
        try {
            JSONObject context = JSONUtil.parseObj(job.getContextJson());
            JSONObject nextStepMeta = context.getJSONObject("_nextStep");
            if (nextStepMeta == null) {
                return;
            }
            // 移除标记以免重复创建
            context.remove("_nextStep");
            job.setContextJson(context.toString());
            jobInstanceService.updateById(job);

            JobStepInstanceEntity nextStep = new JobStepInstanceEntity();
            nextStep.setJobInstanceId(job.getId());
            nextStep.setJobStepTemplateId(nextStepMeta.getLong("templateId"));
            nextStep.setStepCode(nextStepMeta.getStr("stepCode"));
            nextStep.setStepName(nextStepMeta.getStr("stepName"));
            nextStep.setStepType("AUTO");
            nextStep.setStatus("PENDING");
            nextStep.setInputJson(JSONUtil.toJsonStr(nextStepMeta.get("input")));
            nextStep.setPreviousStepId(currentStep.getId());
            nextStep.setCreateTime(new Date());
            stepInstanceService.save(nextStep);
            log.info("已创建下一步骤: stepCode={}, stepId={}", nextStep.getStepCode(), nextStep.getId());
        } catch (Exception e) {
            log.error("从context创建下一步骤失败: jobId={}", job.getId(), e);
        }
    }

}
