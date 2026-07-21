package com.agileboot.domain.social.account;

import cn.hutool.json.JSON;
import com.agileboot.common.core.page.PageDTO;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Business;
import com.agileboot.common.exception.error.ErrorCode.Client;
import com.agileboot.domain.common.command.BulkOperationCommand;
import com.agileboot.domain.social.account.command.SocialAccountAddCommand;
import com.agileboot.domain.social.account.command.SocialAccountUpdateCommand;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.account.db.SocialAccountService;
import com.agileboot.domain.social.account.dto.SocialAccountDTO;
import com.agileboot.domain.social.account.model.SocialAccountModel;
import com.agileboot.domain.social.account.model.SocialAccountModelFactory;
import com.agileboot.domain.social.account.query.SocialAccountQuery;
import com.agileboot.domain.social.client.XhsApiClient;
import com.agileboot.domain.social.client.dto.XhsLoginStatus;
import com.agileboot.domain.social.client.dto.XhsUserBasicInfo;
import com.agileboot.domain.social.client.dto.XhsQrcode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author SocialMedia-Hub
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialAccountApplicationService {

    public static final String PLATFORM_XHS = "xhs";

    private final SocialAccountService accountService;

    private final SocialAccountModelFactory accountModelFactory;

    private final XhsApiClient xhsApiClient;

    public PageDTO<SocialAccountDTO> getAccountList(SocialAccountQuery query) {
        Page<SocialAccountEntity> page = accountService.page(query.toPage(), query.toQueryWrapper());
        List<SocialAccountDTO> records =
            page.getRecords().stream().map(SocialAccountDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    public SocialAccountDTO getAccountInfo(Long id) {
        SocialAccountModel model = accountModelFactory.loadById(id);
        return new SocialAccountDTO(model);
    }

    public void addAccount(SocialAccountAddCommand addCommand) {
        SocialAccountModel model = accountModelFactory.create();
        model.loadAddCommand(addCommand);
        model.checkFields();
        model.insert();
    }

    public void updateAccount(SocialAccountUpdateCommand updateCommand) {
        SocialAccountModel model = accountModelFactory.loadById(updateCommand.getId());
        model.loadUpdateCommand(updateCommand);
        model.checkFields();
        model.updateById();
    }

    public void deleteAccount(BulkOperationCommand<Long> deleteCommand) {
        accountService.removeBatchByIds(deleteCommand.getIds());
    }

    /**
     * 查询账号实时登录状态（透传账号容器）。
     * 已登录时补充真实昵称/小红书号，并把小红书号回写到账号记录。
     */
    public XhsLoginStatus checkLoginStatus(Long accountId) {
        SocialAccountEntity account = loadEnabledXhsAccount(accountId);
        XhsLoginStatus status = xhsApiClient.checkLoginStatus(account.getId());
        if (Boolean.TRUE.equals(status.getIsLoggedIn())) {
            enrichWithProfile(account, status);
        }
        return status;
    }

    private void enrichWithProfile(SocialAccountEntity account, XhsLoginStatus status) {
        try {
            XhsUserBasicInfo basicInfo = xhsApiClient.getMyProfile(account.getId());
            if (basicInfo == null) {
                return;
            }
            status.setNickname(basicInfo.getNickname());
            status.setRedId(basicInfo.getRedId());
            // 小红书号变化时回写（正常只会在首次登录后写一次）
            if (basicInfo.getRedId() != null && !basicInfo.getRedId().equals(account.getXhsUserId())) {
                account.setXhsUserId(basicInfo.getRedId());
                account.updateById();
            }
        } catch (Exception e) {
            // 主页信息获取失败不影响登录状态本身
            log.warn("获取账号 {} 的小红书主页信息失败: {}", account.getId(), e.getMessage());
        }
    }

    /**
     * 获取扫码登录二维码（透传账号容器）
     */
    public XhsQrcode getLoginQrcode(Long accountId) {
        SocialAccountEntity account = loadEnabledXhsAccount(accountId);
        return xhsApiClient.getLoginQrcode(account.getId());
    }

    /**
     * 搜索笔记（透传账号容器，返回原始 data）
     */
    public JSON searchFeeds(Long accountId, String keyword) {
        SocialAccountEntity account = loadEnabledXhsAccount(accountId);
        return xhsApiClient.searchFeeds(account.getId(), keyword);
    }

    private SocialAccountEntity loadEnabledXhsAccount(Long accountId) {
        SocialAccountEntity account = accountService.getById(accountId);
        if (account == null) {
            throw new ApiException(Client.COMMON_REQUEST_PARAMETERS_INVALID, "账号不存在: " + accountId);
        }
        if (account.getStatus() != null && account.getStatus() != 1) {
            throw new ApiException(Client.COMMON_REQUEST_PARAMETERS_INVALID, "账号已停用: " + accountId);
        }
        if (!PLATFORM_XHS.equals(account.getPlatform())) {
            throw new ApiException(Business.COMMON_UNSUPPORTED_OPERATION);
        }
        return account;
    }

}
