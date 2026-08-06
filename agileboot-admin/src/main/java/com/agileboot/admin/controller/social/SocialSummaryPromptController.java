package com.agileboot.admin.controller.social;

import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.prompt.PromptApplicationService;
import com.agileboot.domain.social.prompt.command.PromptAddCommand;
import com.agileboot.domain.social.prompt.command.PromptUpdateCommand;
import com.agileboot.domain.social.prompt.dto.PromptDTO;
import com.agileboot.domain.social.prompt.query.PromptQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "总结提示词API", description = "AI总结提示词配置（按UP主自定义）")
@RestController
@RequestMapping("/social/prompts")
@Validated
@RequiredArgsConstructor
public class SocialSummaryPromptController extends BaseController {

    private final PromptApplicationService promptApplicationService;

    @Operation(summary = "提示词列表")
    @PreAuthorize("@permission.has('social:prompt:list')")
    @GetMapping
    public ResponseDTO<PageDTO<PromptDTO>> list(PromptQuery query) {
        return ResponseDTO.ok(promptApplicationService.getPromptList(query));
    }

    @Operation(summary = "提示词详情")
    @PreAuthorize("@permission.has('social:prompt:list')")
    @GetMapping("/{id}")
    public ResponseDTO<PromptDTO> getInfo(@PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(promptApplicationService.getPromptInfo(id));
    }

    @Operation(summary = "新增提示词")
    @PreAuthorize("@permission.has('social:prompt:add')")
    @PostMapping
    public ResponseDTO<Void> add(@RequestBody @Validated PromptAddCommand command) {
        promptApplicationService.addPrompt(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改提示词")
    @PreAuthorize("@permission.has('social:prompt:edit')")
    @PutMapping("/{id}")
    public ResponseDTO<Void> update(
        @PathVariable @NotNull @Positive Long id,
        @RequestBody @Validated PromptUpdateCommand command
    ) {
        command.setId(id);
        promptApplicationService.updatePrompt(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除提示词")
    @PreAuthorize("@permission.has('social:prompt:delete')")
    @DeleteMapping
    public ResponseDTO<Void> delete(@RequestBody @Validated BulkOperationCommand<Long> command) {
        promptApplicationService.deletePrompt(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "匹配提示词（n8n调用，无鉴权）")
    @GetMapping("/match")
    public ResponseDTO<Map<String, String>> match(
        @RequestParam("upId") @NotBlank String upId,
        @RequestParam("title") String title
    ) {
        String systemPrompt = promptApplicationService.matchPrompt(upId, title);
        Map<String, String> result = new HashMap<>();
        result.put("systemPrompt", systemPrompt);
        result.put("matched", systemPrompt != null ? "true" : "false");
        return ResponseDTO.ok(result);
    }

}
