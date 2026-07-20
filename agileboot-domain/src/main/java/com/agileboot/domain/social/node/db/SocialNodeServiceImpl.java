package com.agileboot.domain.social.node.db;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 住宅节点表 服务实现类
 * </p>
 *
 * @author SocialMedia-Hub
 */
@Service
public class SocialNodeServiceImpl extends ServiceImpl<SocialNodeMapper, SocialNodeEntity>
    implements SocialNodeService {

    @Override
    public SocialNodeEntity getByNodeName(String nodeName) {
        return this.getOne(new QueryWrapper<SocialNodeEntity>().eq("node_name", nodeName));
    }

}
