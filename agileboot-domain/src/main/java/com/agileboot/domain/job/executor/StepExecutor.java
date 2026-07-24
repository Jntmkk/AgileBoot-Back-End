package com.agileboot.domain.job.executor;

import com.agileboot.domain.job.db.JobInstanceEntity;
import com.agileboot.domain.job.db.JobStepInstanceEntity;

/**
 * 步骤执行器接口：每种 executor_capability 对应一个实现。
 * 能力为 "java" 的步骤由调度器在进程内同步执行。
 *
 * @author SocialMedia-Hub
 */
public interface StepExecutor {

    /**
     * 此执行器对应的能力标签（如 "java"、"bili:api"）。
     */
    String capability();

    /**
     * 执行步骤。
     *
     * @param step 步骤实例（状态为PENDING，入参在input_json中）
     * @param job  所属任务实例（含params_json、context_json）
     */
    void execute(JobStepInstanceEntity step, JobInstanceEntity job);

}
