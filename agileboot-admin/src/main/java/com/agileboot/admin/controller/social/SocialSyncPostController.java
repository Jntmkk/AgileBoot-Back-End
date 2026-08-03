package com.agileboot.admin.controller.social;

import com.agileboot.admin.customize.aop.accessLog.AccessLog;
import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.enums.common.BusinessTypeEnum;
import com.agileboot.domain.social.post.SocialSyncPostApplicationService;
import com.agileboot.domain.social.post.dto.SocialSyncPostDTO;
import com.agileboot.domain.social.post.query.SocialSyncPostQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态同步记录管理（B站UP主动态）
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "动态同步API", description = "社交动态同步记录的查询、详情与手动重触发")
@RestController
@RequestMapping("/social/posts")
@Validated
@RequiredArgsConstructor
public class SocialSyncPostController extends BaseController {

    private final SocialSyncPostApplicationService postApplicationService;

    @Operation(summary = "动态列表")
    @PreAuthorize("@permission.has('social:post:list')")
    @GetMapping
    public ResponseDTO<PageDTO<SocialSyncPostDTO>> list(SocialSyncPostQuery query) {
        return ResponseDTO.ok(postApplicationService.getPostList(query));
    }

    @Operation(summary = "动态详情（含转写与总结全文）")
    @PreAuthorize("@permission.has('social:post:query')")
    @GetMapping(value = "/{id}")
    public ResponseDTO<SocialSyncPostDTO> getInfo(@PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(postApplicationService.getPostInfo(id));
    }

    @Operation(summary = "手动重触发转写（重置为待转写，本机 ASR worker 消费）")
    @PreAuthorize("@permission.has('social:post:retrigger')")
    @AccessLog(title = "动态同步", businessType = BusinessTypeEnum.OTHER)
    @PostMapping("/{id}/retriggerTranscribe")
    public ResponseDTO<Void> retriggerTranscribe(@PathVariable @NotNull @Positive Long id) {
        postApplicationService.retriggerTranscribe(id);
        return ResponseDTO.ok();
    }

    @Operation(summary = "手动重触发总结（基于已有转写文本，重置为待总结）")
    @PreAuthorize("@permission.has('social:post:retrigger')")
    @AccessLog(title = "动态同步", businessType = BusinessTypeEnum.OTHER)
    @PostMapping("/{id}/retriggerSummary")
    public ResponseDTO<Void> retriggerSummary(@PathVariable @NotNull @Positive Long id) {
        postApplicationService.retriggerSummary(id);
        return ResponseDTO.ok();
    }

}
