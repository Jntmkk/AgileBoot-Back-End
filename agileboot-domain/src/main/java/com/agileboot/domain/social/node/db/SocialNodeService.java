package com.agileboot.domain.social.node.db;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 住宅节点表 服务类
 * </p>
 *
 * @author SocialMedia-Hub
 */
public interface SocialNodeService extends IService<SocialNodeEntity> {

    SocialNodeEntity getByNodeName(String nodeName);

}
