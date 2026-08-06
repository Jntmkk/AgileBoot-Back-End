package com.agileboot.domain.social.prompt.dto;

import com.agileboot.domain.social.prompt.db.SocialSummaryPromptEntity;
import java.util.Date;
import lombok.Data;

@Data
public class PromptDTO {

    public PromptDTO(SocialSummaryPromptEntity e) {
        if (e == null) {
            return;
        }
        this.id = e.getId() + "";
        this.upId = e.getUpId();
        this.keyword = e.getKeyword();
        this.systemPrompt = e.getSystemPrompt();
        this.sortOrder = e.getSortOrder();
        this.status = e.getStatus();
        this.createTime = e.getCreateTime();
        this.updateTime = e.getUpdateTime();
    }

    private String id;
    private String upId;
    private String keyword;
    private String systemPrompt;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;
    private Date updateTime;

}
