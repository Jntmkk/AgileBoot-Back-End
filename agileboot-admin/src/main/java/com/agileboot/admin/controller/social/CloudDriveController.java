package com.agileboot.admin.controller.social;

import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.social.clouddrive.AlistFileInfo;
import com.agileboot.domain.social.clouddrive.AlistListResult;
import com.agileboot.domain.social.clouddrive.AlistProxyService;
import com.agileboot.domain.social.clouddrive.CloudDriveSyncService;
import com.agileboot.domain.social.config.SocialMediaProperties;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 云盘文件浏览与同步——实时查询 alist，不存储目录结构。
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "云盘文件API", description = "阿里云盘文件浏览与同步触发")
@RestController
@RequestMapping("/social/cloud-drive")
@Validated
@RequiredArgsConstructor
public class CloudDriveController extends BaseController {

    private final AlistProxyService alistProxyService;
    private final CloudDriveSyncService cloudDriveSyncService;
    private final SocialMediaProperties properties;

    /**
     * 浏览目录（实时查 alist，不存库）。
     */
    @Operation(summary = "浏览云盘目录")
    @PreAuthorize("@permission.has('social:cloud-drive:query')")
    @GetMapping("/files")
    public ResponseDTO<AlistListResult> listFiles(
        @RequestParam @NotBlank String path,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "50") @Min(1) int perPage) {
        AlistListResult result = alistProxyService.listFiles(path, page, perPage);
        return ResponseDTO.ok(result);
    }

    /**
     * 获取文件详情。
     */
    @Operation(summary = "获取云盘文件详情")
    @PreAuthorize("@permission.has('social:cloud-drive:query')")
    @GetMapping("/file")
    public ResponseDTO<AlistFileInfo> getFile(@RequestParam @NotBlank String path) {
        return ResponseDTO.ok(alistProxyService.getFile(path));
    }

    /**
     * 同步云盘目录下的新视频到 social_sync_post。
     * 由 n8n 定时调用（带 X-Sync-Token 鉴权），也可从后台手动触发。
     */
    @Hidden
    @Operation(summary = "同步云盘视频（n8n 或后台触发）")
    @PostMapping("/sync")
    public ResponseDTO<Integer> sync(
        @RequestParam @NotBlank String path,
        @RequestHeader(value = "X-Sync-Token", required = false) String token) {
        // n8n 调用需要 X-Sync-Token；后台用户已有 RBAC 鉴权
        if (token != null) {
            String expected = properties.getN8n().getSyncToken();
            if (expected == null || expected.isEmpty() || !expected.equals(token)) {
                throw new ApiException(ErrorCode.Client.COMMON_FORBIDDEN_TO_CALL,
                    "X-Sync-Token 无效");
            }
        }
        int count = cloudDriveSyncService.syncFromAlist(path);
        return ResponseDTO.ok(count);
    }

    /**
     * 同步勾选的视频文件到 social_sync_post。
     */
    @Operation(summary = "同步勾选云盘视频")
    @PostMapping("/sync-selected")
    public ResponseDTO<Integer> syncSelected(
        @RequestBody List<String> paths,
        @RequestHeader(value = "X-Sync-Token", required = false) String token) {
        // n8n 调用需要 X-Sync-Token；后台用户已有 RBAC 鉴权
        if (token != null) {
            String expected = properties.getN8n().getSyncToken();
            if (expected == null || expected.isEmpty() || !expected.equals(token)) {
                throw new ApiException(ErrorCode.Client.COMMON_FORBIDDEN_TO_CALL,
                    "X-Sync-Token 无效");
            }
        }
        int count = cloudDriveSyncService.syncSelectedFiles(paths);
        return ResponseDTO.ok(count);
    }
}
