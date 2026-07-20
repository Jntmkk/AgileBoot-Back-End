package com.agileboot.domain.social.node.dto;

import com.agileboot.domain.social.node.db.SocialNodeEntity;
import java.util.Date;
import lombok.Data;

/**
 * @author SocialMedia-Hub
 */
@Data
public class SocialNodeDTO {

    /**
     * 心跳超时阈值（毫秒）：超过则视为离线
     */
    public static final long OFFLINE_THRESHOLD_MS = 10 * 60 * 1000L;

    public SocialNodeDTO(SocialNodeEntity entity) {
        if (entity != null) {
            this.id = entity.getId() + "";
            this.nodeName = entity.getNodeName();
            this.egressIp = entity.getEgressIp();
            this.ipType = entity.getIpType();
            this.lastHeartbeat = entity.getLastHeartbeat();
            this.status = entity.getStatus();
            this.remark = entity.getRemark();
            this.online = entity.getLastHeartbeat() != null
                && System.currentTimeMillis() - entity.getLastHeartbeat().getTime() < OFFLINE_THRESHOLD_MS;
        }
    }

    private String id;

    private String nodeName;

    private String egressIp;

    private String ipType;

    private Date lastHeartbeat;

    private Integer status;

    private String remark;

    /**
     * 是否在线（10 分钟内有心跳）
     */
    private Boolean online;

}
