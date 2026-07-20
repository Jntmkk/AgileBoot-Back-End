package com.agileboot.domain.social.account.dto;

import com.agileboot.domain.social.account.db.SocialAccountEntity;
import java.util.Date;
import lombok.Data;

/**
 * @author SocialMedia-Hub
 */
@Data
public class SocialAccountDTO {

    public SocialAccountDTO(SocialAccountEntity entity) {
        if (entity != null) {
            this.id = entity.getId() + "";
            this.platform = entity.getPlatform();
            this.accountName = entity.getAccountName();
            this.xhsUserId = entity.getXhsUserId();
            this.nodeName = entity.getNodeName();
            this.proxyUrl = entity.getProxyUrl();
            this.status = entity.getStatus();
            this.remark = entity.getRemark();
            this.createTime = entity.getCreateTime();
            // 端口规则：18060 + id，与节点侧 add-account.sh 一致
            this.port = 18060 + entity.getId().intValue();
        }
    }

    private String id;

    private String platform;

    private String accountName;

    private String xhsUserId;

    private String nodeName;

    private String proxyUrl;

    private Integer status;

    private String remark;

    private Date createTime;

    /**
     * 账号容器端口（= 18060 + id），前端展示用
     */
    private Integer port;

}
