package com.agileboot.admin.controller.social;

import cn.hutool.json.JSON;
import com.agileboot.admin.customize.aop.accessLog.AccessLog;
import com.agileboot.common.core.base.BaseController;
import com.agileboot.common.core.dto.ResponseDTO;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.enums.common.BusinessTypeEnum;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.account.SocialAccountApplicationService;
import com.agileboot.domain.social.account.command.SocialAccountAddCommand;
import com.agileboot.domain.social.account.command.SocialAccountUpdateCommand;
import com.agileboot.domain.social.account.dto.SocialAccountDTO;
import com.agileboot.domain.social.account.query.SocialAccountQuery;
import com.agileboot.domain.social.client.dto.SocialLoginStatus;
import com.agileboot.domain.social.client.dto.SocialQrcode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

/**
 * 社交媒体账号管理
 *
 * @author SocialMedia-Hub
 */
@Tag(name = "社交账号API", description = "社交媒体账号的增删查改与登录态管理")
@RestController
@RequestMapping("/social/accounts")
@Validated
@RequiredArgsConstructor
public class SocialAccountController extends BaseController {

    private final SocialAccountApplicationService accountApplicationService;

    @Operation(summary = "账号列表")
    @PreAuthorize("@permission.has('social:account:list')")
    @GetMapping
    public ResponseDTO<PageDTO<SocialAccountDTO>> list(SocialAccountQuery query) {
        return ResponseDTO.ok(accountApplicationService.getAccountList(query));
    }

    @Operation(summary = "账号详情")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping(value = "/{id}")
    public ResponseDTO<SocialAccountDTO> getInfo(@PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(accountApplicationService.getAccountInfo(id));
    }

    @Operation(summary = "添加账号")
    @PreAuthorize("@permission.has('social:account:add')")
    @AccessLog(title = "社交账号", businessType = BusinessTypeEnum.ADD)
    @PostMapping
    public ResponseDTO<Void> add(@RequestBody SocialAccountAddCommand addCommand) {
        accountApplicationService.addAccount(addCommand);
        return ResponseDTO.ok();
    }

    @Operation(summary = "修改账号")
    @PreAuthorize("@permission.has('social:account:edit')")
    @AccessLog(title = "社交账号", businessType = BusinessTypeEnum.MODIFY)
    @PutMapping("/{id}")
    public ResponseDTO<Void> edit(@PathVariable Long id, @RequestBody SocialAccountUpdateCommand updateCommand) {
        updateCommand.setId(id);
        accountApplicationService.updateAccount(updateCommand);
        return ResponseDTO.ok();
    }

    @Operation(summary = "删除账号")
    @PreAuthorize("@permission.has('social:account:remove')")
    @AccessLog(title = "社交账号", businessType = BusinessTypeEnum.DELETE)
    @DeleteMapping
    public ResponseDTO<Void> remove(@RequestParam List<Long> ids) {
        accountApplicationService.deleteAccount(new BulkOperationCommand<>(ids));
        return ResponseDTO.ok();
    }

    @Operation(summary = "查询账号实时登录状态")
    @PreAuthorize("@permission.has('social:account:login')")
    @GetMapping("/{id}/loginStatus")
    public ResponseDTO<SocialLoginStatus> loginStatus(@PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(accountApplicationService.checkLoginStatus(id));
    }

    @Operation(summary = "获取扫码登录二维码")
    @PreAuthorize("@permission.has('social:account:login')")
    @GetMapping("/{id}/qrcode")
    public ResponseDTO<SocialQrcode> qrcode(@PathVariable @NotNull @Positive Long id) {
        return ResponseDTO.ok(accountApplicationService.getLoginQrcode(id));
    }

    @Operation(summary = "搜索笔记")
    @PreAuthorize("@permission.has('social:account:query')")
    @GetMapping("/{id}/feeds/search")
    public ResponseDTO<JSON> searchFeeds(@PathVariable @NotNull @Positive Long id,
        @RequestParam @NotBlank String keyword) {
        return ResponseDTO.ok(accountApplicationService.searchFeeds(id, keyword));
    }

}
