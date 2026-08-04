package com.agileboot.admin.controller.social;

import com.agileboot.admin.customize.aop.accessLog.AccessLog;
import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.enums.common.BusinessTypeEnum;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.config.SocialMediaProperties;
import com.agileboot.domain.social.follow.BiliSyncService;
import com.agileboot.domain.social.follow.SocialFollowUpApplicationService;
import com.agileboot.domain.social.follow.command.BackfillCommand;
import com.agileboot.domain.social.follow.command.SocialFollowUpAddCommand;
import com.agileboot.domain.social.follow.command.SocialFollowUpUpdateCommand;
import com.agileboot.domain.social.follow.command.SyncByLinkCommand;
import com.agileboot.domain.social.follow.dto.SocialFollowUpDTO;
import com.agileboot.domain.social.follow.query.SocialFollowUpQuery;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 关注UP主管理（动态同步目标）
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "关注UP API", description = "关注UP主管理与动态同步触发")
@RestController
@RequestMapping("/social/follows")
@Validated
@RequiredArgsConstructor
public class SocialFollowUpController extends BaseController {

    private final SocialFollowUpApplicationService followApplicationService;
    private final BiliSyncService biliSyncService;
    private final SocialMediaProperties socialMediaProperties;

    @Operation(summary = "关注UP列表")
    @PreAuthorize("@permission.has('social:follow:list')")
    @GetMapping
    public ResponseDTO<PageDTO<SocialFollowUpDTO>> list(SocialFollowUpQuery query) {
        return ResponseDTO.ok(followApplicationService.getFollowList(query));
    }

    @Operation(summary = "关注UP详情")
    @PreAuthorize("@permission.has('social:follow:query')")
    @GetMapping("/{id}")
    public ResponseDTO<SocialFollowUpDTO> getInfo(
        @PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(followApplicationService.getFollowInfo(id));
    }

    @Operation(summary = "新增关注UP")
    @PreAuthorize("@permission.has('social:follow:add')")
    @AccessLog(title = "关注UP", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<Void> add(
        @Validated @RequestBody SocialFollowUpAddCommand addCommand) {
        followApplicationService.addFollow(addCommand);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改关注UP")
    @PreAuthorize("@permission.has('social:follow:edit')")
    @AccessLog(title = "关注UP", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{id}")
    public ResponseDTO<Void> edit(
        @PathVariable Long id,
        @Validated @RequestBody SocialFollowUpUpdateCommand updateCommand) {
        updateCommand.setId(id);
        followApplicationService.updateFollow(updateCommand);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除关注UP")
    @PreAuthorize("@permission.has('social:follow:remove')")
    @AccessLog(title = "关注UP", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping
    public ResponseDTO<Void> remove(@RequestParam List<Long> ids) {
        followApplicationService.deleteFollow(new BulkOperationCommand<>(ids));
        return ResponseDTO.ok();
    }

    @Operation(summary = "同步指定链接（粘贴B站动态/视频链接）")
    @PreAuthorize("@permission.has('social:follow:sync')")
    @AccessLog(title = "动态同步", businessType = BusinessTypeEnum.OTHER)
    @PostMapping("/syncByLink")
    public ResponseDTO<Void> syncByLink(
        @Validated @RequestBody SyncByLinkCommand command) {
        followApplicationService.syncByLink(command);
        return ResponseDTO.ok();
    }

    @Operation(summary = "按时间范围补数据")
    @PreAuthorize("@permission.has('social:follow:sync')")
    @AccessLog(title = "动态同步", businessType = BusinessTypeEnum.OTHER)
    @PostMapping("/backfill")
    public ResponseDTO<Void> backfill(
        @Validated @RequestBody BackfillCommand command) {
        followApplicationService.backfill(command);
        return ResponseDTO.ok();
    }

    @Hidden
    @Operation(summary = "n8n 触发 feed 同步")
    @PostMapping("/syncFeed")
    public ResponseDTO<Void> syncFeed(
        @RequestHeader("X-Sync-Token") String token) {
        if (!socialMediaProperties.getN8n().getSyncToken().equals(token)) {
            throw new ApiException(ErrorCode.Client.COMMON_FORBIDDEN_TO_CALL, "X-Sync-Token 无效");
        }
        biliSyncService.syncFeed();
        return ResponseDTO.ok();
    }

}
