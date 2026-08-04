package com.agileboot.domain.social.follow.command;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * 同步指定链接命令
 *
 * @author SocialMedia-Hub
 */
@Data
public class SyncByLinkCommand {

    @NotBlank(message = "平台不能为空")
    private String platform;

    @NotBlank(message = "链接不能为空")
    @Size(max = 1024, message = "链接不能超过1024个字符")
    private String url;

}
