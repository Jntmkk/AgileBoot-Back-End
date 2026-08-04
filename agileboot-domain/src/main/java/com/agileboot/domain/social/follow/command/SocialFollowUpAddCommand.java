package com.agileboot.domain.social.follow.command;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 新增关注UP主命令
 *
 * @author SocialMedia-Hub
 */
@Data
public class SocialFollowUpAddCommand {

    @NotBlank(message = "平台不能为空")
    protected String platform;

    @NotBlank(message = "UP主ID不能为空")
    @Size(max = 64, message = "UP主ID不能超过64个字符")
    protected String upId;

    @NotBlank(message = "UP主昵称不能为空")
    @Size(max = 128, message = "UP主昵称不能超过128个字符")
    protected String upName;

    @Size(max = 512, message = "头像链接不能超过512个字符")
    protected String upAvatar;

    protected Integer status;

    protected Integer syncEnabled;

    @Size(max = 500, message = "备注不能超过500个字符")
    protected String remark;

}
