package com.agileboot.domain.social.credential.db;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 社交平台登录凭据表 服务实现类
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Service
public class SocialCredentialServiceImpl extends ServiceImpl<SocialCredentialMapper, SocialCredentialEntity>
    implements SocialCredentialService {

    @Override
    public SocialCredentialEntity getByAccountId(Long accountId) {
        if (accountId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SocialCredentialEntity>()
            .eq(SocialCredentialEntity::getAccountId, accountId), false);
    }

}
