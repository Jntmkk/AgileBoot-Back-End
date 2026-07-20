package com.agileboot.admin.controller.social;

import cn.hutool.core.util.StrUtil;
import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Client;
import com.agileboot.domain.social.config.SocialMediaProperties;
import com.agileboot.domain.social.node.SocialNodeApplicationService;
import com.agileboot.domain.social.node.command.NodeHeartbeatCommand;
import com.agileboot.domain.social.node.db.SocialNodeEntity;
import com.agileboot.domain.social.node.dto.SocialNodeDTO;
import com.agileboot.domain.social.node.query.SocialNodeQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 住宅节点管理 + 节点心跳
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "住宅节点API", description = "住宅节点列表与节点 agent 心跳上报")
@RestController
@RequestMapping("/social/nodes")
@Validated
@RequiredArgsConstructor
public class SocialNodeController extends BaseController {

    public static final String NODE_TOKEN_HEADER = "X-Node-Token";

    private final SocialNodeApplicationService nodeApplicationService;

    private final SocialMediaProperties socialMediaProperties;

    @Operation(summary = "节点列表")
    @PreAuthorize("@permission.has('social:node:list')")
    @GetMapping
    public ResponseDTO<PageDTO<SocialNodeDTO>> list(SocialNodeQuery query) {
        return ResponseDTO.ok(nodeApplicationService.getNodeList(query));
    }

    @Operation(summary = "修改节点（状态/备注）")
    @PreAuthorize("@permission.has('social:node:edit')")
    @PutMapping
    public ResponseDTO<Void> edit(@RequestBody SocialNodeEntity update) {
        nodeApplicationService.updateNode(update);
        return ResponseDTO.ok();
    }

    /**
     * 节点 agent 心跳上报。
     * 走 SecurityConfig 的 permitAll 白名单，用 X-Node-Token 头鉴权。
     */
    @Operation(summary = "节点心跳（agent 调用）")
    @PostMapping("/heartbeat")
    public ResponseDTO<Void> heartbeat(@RequestHeader(NODE_TOKEN_HEADER) String token,
        @Valid @RequestBody NodeHeartbeatCommand command) {
        if (!StrUtil.equals(socialMediaProperties.getNodeToken(), token)) {
            throw new ApiException(Client.COMMON_NO_AUTHORIZATION, "/social/nodes/heartbeat");
        }
        nodeApplicationService.heartbeat(command);
        return ResponseDTO.ok();
    }

}
