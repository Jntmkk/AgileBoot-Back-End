package com.agileboot.domain.social.node.command;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 节点心跳报文（节点 agent → 后端）
 *
 * @author SocialMedia-Hub
 */
@Data
public class NodeHeartbeatCommand {

    @NotBlank(message = "节点标识不能为空")
    private String nodeName;

    private String egressIp;

}
