package com.agileboot.domain.social.node;

import com.agileboot.common.core.page.PageDTO;
import com.agileboot.domain.social.node.command.NodeHeartbeatCommand;
import com.agileboot.domain.social.node.db.SocialNodeEntity;
import com.agileboot.domain.social.node.db.SocialNodeService;
import com.agileboot.domain.social.node.dto.SocialNodeDTO;
import com.agileboot.domain.social.node.query.SocialNodeQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author SocialMedia-Hub
 */
@Service
@RequiredArgsConstructor
public class SocialNodeApplicationService {

    private final SocialNodeService nodeService;

    public PageDTO<SocialNodeDTO> getNodeList(SocialNodeQuery query) {
        Page<SocialNodeEntity> page = nodeService.page(query.toPage(), query.toQueryWrapper());
        List<SocialNodeDTO> records =
            page.getRecords().stream().map(SocialNodeDTO::new).collect(Collectors.toList());
        return new PageDTO<>(records, page.getTotal());
    }

    /**
     * 心跳：节点首次上报自动注册（upsert），后续刷新出口 IP 与心跳时间
     */
    public void heartbeat(NodeHeartbeatCommand command) {
        SocialNodeEntity node = nodeService.getByNodeName(command.getNodeName());
        if (node == null) {
            node = new SocialNodeEntity();
            node.setNodeName(command.getNodeName());
            node.setIpType("residential");
            node.setStatus(1);
            node.setEgressIp(command.getEgressIp());
            node.setLastHeartbeat(new Date());
            node.insert();
        } else {
            node.setEgressIp(command.getEgressIp());
            node.setLastHeartbeat(new Date());
            node.updateById();
        }
    }

    public void updateNode(SocialNodeEntity update) {
        SocialNodeEntity node = nodeService.getById(update.getId());
        if (node != null) {
            node.setStatus(update.getStatus());
            node.setRemark(update.getRemark());
            node.updateById();
        }
    }

}
