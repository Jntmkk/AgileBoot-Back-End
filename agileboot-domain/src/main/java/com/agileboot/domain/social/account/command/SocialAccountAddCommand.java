package com.agileboot.domain.social.account.command;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * @author SocialMedia-Hub
 */
@Data
public class SocialAccountAddCommand {

    @NotBlank(message = "平台不能为空")
    protected String platform;

    @NotBlank(message = "账号备注名不能为空")
    @Size(max = 64, message = "账号备注名不能超过64个字符")
    protected String accountName;

    protected String nodeName;

    protected String proxyUrl;

    protected Integer status;

    protected String remark;

}
