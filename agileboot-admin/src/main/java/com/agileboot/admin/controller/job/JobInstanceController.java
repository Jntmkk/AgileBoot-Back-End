package com.agileboot.admin.controller.job;

import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.domain.job.JobOrchestrationApplicationService;
import com.agileboot.domain.job.command.CreateJobInstanceCommand;
import com.agileboot.domain.job.dto.JobInstanceDTO;
import com.agileboot.domain.job.query.JobInstanceQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务编排API（用户面）。
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "任务编排API", description = "任务实例的创建与查询")
@RestController
@RequestMapping("/job/instances")
@Validated
@RequiredArgsConstructor
public class JobInstanceController extends BaseController {

    private final JobOrchestrationApplicationService orchestrationService;

    @Operation(summary = "创建任务实例")
    @PreAuthorize("@permission.has('social:account:query')")
    @PostMapping
    public ResponseDTO<JobInstanceDTO> create(@RequestBody @Valid CreateJobInstanceCommand command) {
        return ResponseDTO.ok(orchestrationService.createJobInstance(command));
    }

    @Operation(summary = "任务实例列表")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping
    public ResponseDTO<PageDTO<JobInstanceDTO>> list(JobInstanceQuery query) {
        return ResponseDTO.ok(orchestrationService.listInstances(query));
    }

    @Operation(summary = "任务实例详情（含步骤列表）")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping("/{id}")
    public ResponseDTO<JobInstanceDTO> detail(
        @PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(orchestrationService.getInstanceDetail(id));
    }

}
