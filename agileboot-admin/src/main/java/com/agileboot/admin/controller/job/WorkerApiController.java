package com.agileboot.admin.controller.job;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Client;
import com.agileboot.common.exception.error.ErrorCode.Internal;
import com.agileboot.domain.job.config.JobOrchestrationProperties;
import com.agileboot.domain.job.db.JobArtifactEntity;
import com.agileboot.domain.job.db.JobArtifactService;
import com.agileboot.domain.job.db.JobInstanceEntity;
import com.agileboot.domain.job.db.JobInstanceService;
import com.agileboot.domain.job.db.JobNodeEntity;
import com.agileboot.domain.job.db.JobNodeService;
import com.agileboot.domain.job.db.JobStepInstanceEntity;
import com.agileboot.domain.job.db.JobStepInstanceService;
import com.agileboot.domain.job.db.JobStepTemplateEntity;
import com.agileboot.domain.job.db.JobStepTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Worker 面向调度器的 HTTP API：拉任务、回调、产物上传、心跳。
 * 所有请求需 X-Node-Token 鉴权。
 *
 * @author SocialMedia-Hub
 */
@Slf4j
@Tag(name = "Worker API", description = "Node Worker 拉任务/回调/上传/心跳")
@RestController
@RequestMapping("/api/v1/worker")
@Validated
@RequiredArgsConstructor
public class WorkerApiController {

    private static final String HEADER_NODE_TOKEN = "X-Node-Token";

    private final JobNodeService nodeService;

    private final JobStepInstanceService stepInstanceService;

    private final JobStepTemplateService stepTemplateService;

    private final JobInstanceService jobInstanceService;

    private final JobArtifactService artifactService;

    private final JobOrchestrationProperties properties;

    /**
     * 验证X-Node-Token并返回对应节点。
     */
    private JobNodeEntity authenticate(@RequestHeader(HEADER_NODE_TOKEN) String token) {
        if (token == null || token.isEmpty()) {
            throw new ApiException(Client.COMMON_NO_AUTHORIZATION, "缺少X-Node-Token");
        }
        JobNodeEntity node = nodeService.getOne(
            new LambdaQueryWrapper<JobNodeEntity>()
                .eq(JobNodeEntity::getToken, token), false);
        if (node == null) {
            throw new ApiException(Client.COMMON_NO_AUTHORIZATION, "无效的Node-Token");
        }
        return node;
    }

    @Operation(summary = "长轮询拉取任务")
    @GetMapping("/tasks/next")
    public ResponseDTO<Map<String, Object>> pullTask(
        @RequestHeader(HEADER_NODE_TOKEN) String token,
        @RequestParam(defaultValue = "bili:download") String capabilities,
        @RequestParam(defaultValue = "30") int timeoutSeconds) {

        JobNodeEntity node = authenticate(token);
        List<String> capabilityList = parseCapabilities(capabilities);

        // 查所有PENDING步骤
        List<JobStepInstanceEntity> pendingSteps = stepInstanceService.list(
            new LambdaQueryWrapper<JobStepInstanceEntity>()
                .eq(JobStepInstanceEntity::getStatus, "PENDING")
                .eq(JobStepInstanceEntity::getStepType, "AUTO")
                .orderByAsc(JobStepInstanceEntity::getCreateTime));

        for (JobStepInstanceEntity step : pendingSteps) {
            // 查步骤模板获取capability
            JobStepTemplateEntity tmpl = stepTemplateService.getById(step.getJobStepTemplateId());
            if (tmpl == null || tmpl.getExecutorCapability() == null) {
                continue;
            }
            // 检查节点能力是否匹配
            if (!capabilityList.contains(tmpl.getExecutorCapability())) {
                continue;
            }
            // 检查依赖是否满足
            if (!areDependenciesSatisfied(step, tmpl)) {
                continue;
            }
            // 原子认领
            boolean claimed = stepInstanceService.update(
                new LambdaUpdateWrapper<JobStepInstanceEntity>()
                    .eq(JobStepInstanceEntity::getId, step.getId())
                    .eq(JobStepInstanceEntity::getStatus, "PENDING")
                    .set(JobStepInstanceEntity::getStatus, "RUNNING")
                    .set(JobStepInstanceEntity::getAssignedNodeId, node.getNodeId())
                    .set(JobStepInstanceEntity::getStartedAt, new Date()));
            if (!claimed) {
                continue;
            }
            // 更新节点负载
            nodeService.update(new LambdaUpdateWrapper<JobNodeEntity>()
                .eq(JobNodeEntity::getNodeId, node.getNodeId())
                .setSql("current_load = current_load + 1"));

            // 查任务实例获取更多上下文
            JobInstanceEntity job = jobInstanceService.getById(step.getJobInstanceId());

            // 构建响应
            Map<String, Object> task = new HashMap<>();
            task.put("stepInstanceId", step.getId());
            task.put("jobInstanceId", step.getJobInstanceId());
            task.put("stepCode", step.getStepCode());
            task.put("stepName", step.getStepName());
            task.put("input", step.getInputJson() != null
                ? JSONUtil.parse(step.getInputJson()) : null);
            Map<String, String> artifactUrls = new HashMap<>();
            if (step.getInputArtifactIds() != null) {
                JSONArray ids = JSONUtil.parseArray(step.getInputArtifactIds());
                for (Object id : ids) {
                    Long artifactId = Long.valueOf(id.toString());
                    artifactUrls.put("artifact_" + artifactId,
                        "/api/v1/worker/artifacts/" + artifactId + "/download?token=" + token);
                }
            }
            task.put("inputArtifactUrls", artifactUrls);
            log.info("节点 {} 认领步骤: stepId={}, stepCode={}",
                node.getNodeId(), step.getId(), step.getStepCode());
            return ResponseDTO.ok(task);
        }
        // 无任务
        return ResponseDTO.ok(null);
    }

    @Operation(summary = "执行回调（COMPLETED/FAILED/PROGRESS）")
    @PostMapping("/callback")
    public ResponseDTO<Void> callback(
        @RequestHeader(HEADER_NODE_TOKEN) String token,
        @RequestBody Map<String, Object> body) {

        JobNodeEntity node = authenticate(token);
        Long stepInstanceId = Long.valueOf(body.get("stepInstanceId").toString());
        String status = (String) body.get("status");

        JobStepInstanceEntity step = stepInstanceService.getById(stepInstanceId);
        if (step == null) {
            throw new ApiException(Internal.INTERNAL_ERROR, "步骤实例不存在: " + stepInstanceId);
        }
        if (!node.getNodeId().equals(step.getAssignedNodeId())) {
            log.warn("节点 {} 试图回调非自己执行的步骤 {}", node.getNodeId(), stepInstanceId);
            throw new ApiException(Client.COMMON_FORBIDDEN_TO_CALL, "无权操作此步骤");
        }

        // 终态幂等
        if ("COMPLETED".equals(step.getStatus()) || "FAILED".equals(step.getStatus())) {
            log.info("步骤已终态，忽略重复回调: stepId={}, status={}", stepInstanceId, step.getStatus());
            return ResponseDTO.ok();
        }

        step.setStatus(status);
        step.setEndedAt(new Date());

        if (body.containsKey("output")) {
            step.setOutputJson(JSONUtil.toJsonStr(body.get("output")));
        }
        if (body.containsKey("outputArtifactIds")) {
            step.setOutputArtifactIds(JSONUtil.toJsonStr(body.get("outputArtifactIds")));
        }
        if ("FAILED".equals(status) && body.containsKey("error")) {
            step.setErrorMsg(body.get("error").toString());
        }
        stepInstanceService.updateById(step);

        // 更新节点负载
        nodeService.update(new LambdaUpdateWrapper<JobNodeEntity>()
            .eq(JobNodeEntity::getNodeId, node.getNodeId())
            .setSql("current_load = GREATEST(current_load - 1, 0)"));

        // 如果成功，合并输出到job.context_json，推进任务
        if ("COMPLETED".equals(status)) {
            JobInstanceEntity job = jobInstanceService.getById(step.getJobInstanceId());
            if (job != null) {
                JSONObject context = job.getContextJson() != null
                    ? JSONUtil.parseObj(job.getContextJson()) : new JSONObject();
                context.set("_step_" + step.getStepCode(),
                    step.getOutputJson() != null ? JSONUtil.parse(step.getOutputJson()) : null);
                context.remove("_nextStep"); // 清除临时标记
                job.setContextJson(context.toString());
                // 如果没有更多步骤，标记任务完成
                long remaining = stepInstanceService.count(
                    new LambdaQueryWrapper<JobStepInstanceEntity>()
                        .eq(JobStepInstanceEntity::getJobInstanceId, job.getId())
                        .ne(JobStepInstanceEntity::getStatus, "COMPLETED")
                        .ne(JobStepInstanceEntity::getStatus, "FAILED"));
                if (remaining == 0) {
                    job.setStatus("COMPLETED");
                    job.setEndTime(new Date());
                    log.info("任务完成: jobId={}", job.getId());
                }
                jobInstanceService.updateById(job);
            }
        }

        log.info("Worker回调: nodeId={}, stepId={}, status={}", node.getNodeId(), stepInstanceId, status);
        return ResponseDTO.ok();
    }

    @Operation(summary = "产物上传（multipart）")
    @PostMapping("/artifacts")
    public ResponseDTO<Map<String, Object>> uploadArtifact(
        @RequestHeader(HEADER_NODE_TOKEN) String token,
        @RequestParam Long stepInstanceId,
        @RequestParam String artifactType,
        @RequestPart("file") MultipartFile file) throws IOException {

        JobNodeEntity node = authenticate(token);
        JobStepInstanceEntity step = stepInstanceService.getById(stepInstanceId);
        if (step == null) {
            throw new ApiException(Internal.INTERNAL_ERROR, "步骤实例不存在: " + stepInstanceId);
        }

        // 存储路径：{baseDir}/{jobInstanceId}/{stepCode}/{uuid}-{originalFilename}
        String dir = properties.getArtifactBaseDir()
            + "/" + step.getJobInstanceId()
            + "/" + step.getStepCode();
        Path dirPath = Paths.get(dir);
        Files.createDirectories(dirPath);
        String storedName = UUID.randomUUID().toString().substring(0, 8)
            + "-" + file.getOriginalFilename();
        Path filePath = dirPath.resolve(storedName);
        file.transferTo(filePath.toFile());

        // 创建产物记录
        JobArtifactEntity artifact = new JobArtifactEntity();
        artifact.setJobInstanceId(step.getJobInstanceId());
        artifact.setJobStepInstanceId(stepInstanceId);
        artifact.setArtifactType(artifactType);
        artifact.setStorageType("local");
        artifact.setFilePath(filePath.toString());
        artifact.setFileSize(file.getSize());
        artifact.setStatus(1);
        artifact.setCreateTime(new Date());
        artifactService.save(artifact);

        Map<String, Object> result = new HashMap<>();
        result.put("artifactId", artifact.getId());
        result.put("storageType", "local");
        log.info("产物上传完成: artifactId={}, file={}, size={}",
            artifact.getId(), storedName, file.getSize());
        return ResponseDTO.ok(result);
    }

    @Operation(summary = "节点心跳上报")
    @PostMapping("/heartbeat")
    public ResponseDTO<Void> heartbeat(
        @RequestHeader(HEADER_NODE_TOKEN) String token,
        @RequestBody Map<String, Object> body) {

        JobNodeEntity node = authenticate(token);
        if (body.containsKey("currentLoad")) {
            node.setCurrentLoad(Integer.valueOf(body.get("currentLoad").toString()));
        }
        if (body.containsKey("status")) {
            node.setStatus(body.get("status").toString());
        }
        if (body.containsKey("ipAddress")) {
            node.setIpAddress(body.get("ipAddress").toString());
        }
        node.setLastHeartbeat(new Date());
        nodeService.updateById(node);
        return ResponseDTO.ok();
    }

    @Operation(summary = "产物下载")
    @GetMapping("/artifacts/{artifactId}/download")
    public ResponseDTO<Map<String, String>> downloadArtifact(
        @RequestHeader(HEADER_NODE_TOKEN) String token,
        @PathVariable Long artifactId) {

        authenticate(token);
        JobArtifactEntity artifact = artifactService.getById(artifactId);
        if (artifact == null) {
            throw new ApiException(Internal.INTERNAL_ERROR, "产物不存在: " + artifactId);
        }
        Map<String, String> result = new HashMap<>();
        result.put("filePath", artifact.getFilePath());
        result.put("content", artifact.getContent());
        return ResponseDTO.ok(result);
    }

    private List<String> parseCapabilities(String capabilities) {
        return Arrays.stream(capabilities.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    }

    private boolean areDependenciesSatisfied(JobStepInstanceEntity step,
        JobStepTemplateEntity template) {
        if (template.getDependencyStepCodes() == null) {
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
}
