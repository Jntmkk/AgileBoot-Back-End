package com.agileboot.domain.social.credential.db;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 社交平台登录凭据表 服务类
 * </p>
 *
 * @author SocialMedia-Hub
 */
public interface SocialCredentialService extends IService<SocialCredentialEntity> {

    /**
     * 按账号查询凭据（一对一，可能为null）
     */
    SocialCredentialEntity getByAccountId(Long accountId);

}
