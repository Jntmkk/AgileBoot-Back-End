package com.agileboot.admin.controller.job;

import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.domain.job.db.JobTemplateEntity;
import com.agileboot.domain.job.db.JobTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "任务模板API", description = "任务模板的查询")
@RestController
@RequestMapping("/job/templates")
@Validated
@RequiredArgsConstructor
public class JobTemplateController {

    private final JobTemplateService templateService;

    @Operation(summary = "模板列表")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping
    public ResponseDTO<List<JobTemplateEntity>> list() {
        return ResponseDTO.ok(templateService.list());
    }

    @Operation(summary = "新增模板")
    @PreAuthorize("@permission.has('social:account:query')")
    @PostMapping
    public ResponseDTO<Void> add(@RequestBody Map<String, Object> body) {
        JobTemplateEntity entity = new JobTemplateEntity();
        entity.setTemplateCode((String) body.get("templateCode"));
        entity.setTemplateName((String) body.get("templateName"));
        entity.setBizType((String) body.get("bizType"));
        entity.setDescription((String) body.getOrDefault("description", ""));
        entity.setStatus(1);
        entity.setVersion(1);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        templateService.save(entity);
        return ResponseDTO.ok();
    }

}
