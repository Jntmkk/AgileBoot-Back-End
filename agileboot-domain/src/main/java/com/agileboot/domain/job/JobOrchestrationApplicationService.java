package com.agileboot.domain.job;

import cn.hutool.json.JSONUtil;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Business;
import com.agileboot.common.exception.error.ErrorCode.Internal;
import com.agileboot.domain.job.command.CreateJobInstanceCommand;
import com.agileboot.domain.job.db.JobInstanceEntity;
import com.agileboot.domain.job.db.JobInstanceService;
import com.agileboot.domain.job.db.JobStepInstanceEntity;
import com.agileboot.domain.job.db.JobStepInstanceService;
import com.agileboot.domain.job.db.JobStepTemplateEntity;
import com.agileboot.domain.job.db.JobStepTemplateService;
import com.agileboot.domain.job.db.JobTemplateEntity;
import com.agileboot.domain.job.db.JobTemplateService;
import com.agileboot.domain.job.dto.JobInstanceDTO;
import com.agileboot.domain.job.dto.JobStepInstanceDTO;
import com.agileboot.domain.job.query.JobInstanceQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Comparator;
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
public class JobOrchestrationApplicationService {

    private final JobTemplateService templateService;

    private final JobStepTemplateService stepTemplateService;

    private final JobInstanceService instanceService;

    private final JobStepInstanceService stepInstanceService;

    @Transactional
    public JobInstanceDTO createJobInstance(CreateJobInstanceCommand command) {
        // 1. 查模板
        JobTemplateEntity template = templateService.getOne(
            new LambdaQueryWrapper<JobTemplateEntity>()
                .eq(JobTemplateEntity::getTemplateCode, command.getTemplateCode())
                .eq(JobTemplateEntity::getDeleted, false), false);
        if (template == null) {
            throw new ApiException(Business.COMMON_OBJECT_NOT_FOUND,
                command.getTemplateCode(), "任务模板");
        }

        // 2. 创建任务实例
        JobInstanceEntity instance = new JobInstanceEntity();
        instance.setJobTemplateId(template.getId());
        instance.setBizType(template.getBizType());
        instance.setBizKey(command.getBizKey());
        instance.setBizSubKey(command.getBizSubKey());
        instance.setParamsJson(command.getParamsJson());
        instance.setStatus("PENDING");
        instance.setTriggerSource("manual");
        instance.setCreateTime(new Date());
        instanceService.save(instance);
        log.info("创建任务实例: id={}, templateCode={}, bizKey={}",
            instance.getId(), command.getTemplateCode(), command.getBizKey());

        // 3. 查第一个步骤模板（order_index最小的）
        List<JobStepTemplateEntity> stepTemplates = stepTemplateService.list(
            new LambdaQueryWrapper<JobStepTemplateEntity>()
                .eq(JobStepTemplateEntity::getJobTemplateId, template.getId())
                .eq(JobStepTemplateEntity::getDeleted, false)
                .orderByAsc(JobStepTemplateEntity::getOrderIndex));
        if (stepTemplates.isEmpty()) {
            throw new ApiException(Internal.INTERNAL_ERROR, "模板无步骤定义");
        }
        JobStepTemplateEntity firstStep = stepTemplates.get(0);

        // 4. 创建第一个步骤实例
        JobStepInstanceEntity step = new JobStepInstanceEntity();
        step.setJobInstanceId(instance.getId());
        step.setJobStepTemplateId(firstStep.getId());
        step.setStepCode(firstStep.getStepCode());
        step.setStepName(firstStep.getStepName());
        step.setStepType(firstStep.getStepType());
        step.setStatus("PENDING");
        step.setCreateTime(new Date());
        stepInstanceService.save(step);
        log.info("创建首个步骤实例: stepId={}, stepCode={}", step.getId(), firstStep.getStepCode());

        return toDTO(instance, List.of(step));
    }

    public PageDTO<JobInstanceDTO> listInstances(JobInstanceQuery query) {
        LambdaQueryWrapper<JobInstanceEntity> wrapper = new LambdaQueryWrapper<>();
        if (query.getBizKey() != null) {
            wrapper.eq(JobInstanceEntity::getBizKey, query.getBizKey());
        }
        if (query.getStatus() != null) {
            wrapper.eq(JobInstanceEntity::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(JobInstanceEntity::getCreateTime);

        Page<JobInstanceEntity> page = instanceService.page(query.toPage(), wrapper);

        List<JobInstanceDTO> dtos = page.getRecords().stream()
            .map(e -> toDTO(e, null))
            .collect(Collectors.toList());
        return new PageDTO<>(dtos, page.getTotal());
    }

    public JobInstanceDTO getInstanceDetail(Long id) {
        JobInstanceEntity instance = instanceService.getById(id);
        if (instance == null) {
            throw new ApiException(Business.COMMON_OBJECT_NOT_FOUND, id.toString(), "任务实例");
        }
        List<JobStepInstanceEntity> steps = stepInstanceService.list(
            new LambdaQueryWrapper<JobStepInstanceEntity>()
                .eq(JobStepInstanceEntity::getJobInstanceId, id)
                .orderByAsc(JobStepInstanceEntity::getCreateTime));
        return toDTO(instance, steps);
    }

    private JobInstanceDTO toDTO(JobInstanceEntity entity, List<JobStepInstanceEntity> steps) {
        JobInstanceDTO dto = new JobInstanceDTO();
        dto.setId(entity.getId());
        dto.setJobTemplateId(entity.getJobTemplateId());
        dto.setBizType(entity.getBizType());
        dto.setBizKey(entity.getBizKey());
        dto.setBizSubKey(entity.getBizSubKey());
        dto.setParamsJson(entity.getParamsJson());
        dto.setStatus(entity.getStatus());
        dto.setCurrentStepCode(entity.getCurrentStepCode());
        dto.setContextJson(entity.getContextJson());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setErrorMsg(entity.getErrorMsg());
        dto.setTriggerSource(entity.getTriggerSource());
        dto.setCreateTime(entity.getCreateTime());

        // 查模板名称
        if (entity.getJobTemplateId() != null) {
            JobTemplateEntity tmpl = templateService.getById(entity.getJobTemplateId());
            if (tmpl != null) {
                dto.setTemplateCode(tmpl.getTemplateCode());
                dto.setTemplateName(tmpl.getTemplateName());
            }
        }

        if (steps != null) {
            dto.setSteps(steps.stream()
                .map(this::toStepDTO)
                .collect(Collectors.toList()));
        }
        return dto;
    }

    private JobStepInstanceDTO toStepDTO(JobStepInstanceEntity entity) {
        JobStepInstanceDTO dto = new JobStepInstanceDTO();
        dto.setId(entity.getId());
        dto.setJobInstanceId(entity.getJobInstanceId());
        dto.setStepCode(entity.getStepCode());
        dto.setStepName(entity.getStepName());
        dto.setStepType(entity.getStepType());
        dto.setStatus(entity.getStatus());
        dto.setAssignedNodeId(entity.getAssignedNodeId());
        dto.setInputJson(entity.getInputJson());
        dto.setOutputJson(entity.getOutputJson());
        dto.setInputArtifactIds(entity.getInputArtifactIds());
        dto.setOutputArtifactIds(entity.getOutputArtifactIds());
        dto.setStartedAt(entity.getStartedAt());
        dto.setEndedAt(entity.getEndedAt());
        dto.setRetryCount(entity.getRetryCount());
        dto.setErrorMsg(entity.getErrorMsg());
        dto.setPreviousStepId(entity.getPreviousStepId());
        return dto;
    }

}
