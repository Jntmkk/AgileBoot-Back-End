package com.agileboot.domain.social.account.model;

import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import com.agileboot.domain.social.account.db.SocialAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 社交账号模型工厂
 *
 * @author SocialMedia-Hub
 */
@Component
@RequiredArgsConstructor
public class SocialAccountModelFactory {

    private final SocialAccountService accountService;

    public SocialAccountModel loadById(Long id) {
        SocialAccountEntity byId = accountService.getById(id);

        if (byId == null) {
            throw new ApiException(ErrorCode.Business.COMMON_OBJECT_NOT_FOUND, id, "社交账号");
        }

        return new SocialAccountModel(byId);
    }

    public SocialAccountModel create() {
        return new SocialAccountModel();
    }

}
