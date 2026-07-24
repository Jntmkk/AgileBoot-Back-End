package com.agileboot.admin.controller.job;

import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.domain.job.db.JobNodeEntity;
import com.agileboot.domain.job.db.JobNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "任务节点API", description = "任务执行节点的查询")
@RestController
@RequestMapping("/job/nodes")
@Validated
@RequiredArgsConstructor
public class JobNodeController {

    private final JobNodeService nodeService;

    @Operation(summary = "节点列表")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping
    public ResponseDTO<List<JobNodeEntity>> list() {
        return ResponseDTO.ok(nodeService.list());
    }

}
