package com.agileboot.domain.social.account.model;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.agileboot.common.exception.ApiException;
import com.agileboot.common.exception.error.ErrorCode.Client;
import com.agileboot.domain.social.account.command.SocialAccountAddCommand;
import com.agileboot.domain.social.account.command.SocialAccountUpdateCommand;
import com.agileboot.domain.social.account.db.SocialAccountEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class SocialAccountModel extends SocialAccountEntity {

    public SocialAccountModel(SocialAccountEntity entity) {
        if (entity != null) {
            BeanUtil.copyProperties(entity, this);
        }
    }

    public void loadAddCommand(SocialAccountAddCommand command) {
        if (command != null) {
            BeanUtil.copyProperties(command, this, "id");
        }
    }

    public void loadUpdateCommand(SocialAccountUpdateCommand command) {
        if (command != null) {
            loadAddCommand(command);
        }
    }

    public void checkFields() {
        if (StrUtil.isBlank(getPlatform())) {
            throw new ApiException(Client.COMMON_REQUEST_PARAMETERS_INVALID, "平台不能为空");
        }
        if (StrUtil.isBlank(getAccountName())) {
            throw new ApiException(Client.COMMON_REQUEST_PARAMETERS_INVALID, "账号备注名不能为空");
        }
    }

}
