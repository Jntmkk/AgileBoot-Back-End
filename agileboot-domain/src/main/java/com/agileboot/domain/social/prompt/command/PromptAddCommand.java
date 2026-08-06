package com.agileboot.domain.social.prompt.command;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class PromptAddCommand {

    @NotBlank(message = "UP ID不能为空")
    @Size(max = 64)
    private String upId;

    @NotBlank(message = "关键词不能为空")
    @Size(max = 255)
    private String keyword;

    @NotBlank(message = "提示词不能为空")
    private String systemPrompt;

    private Integer sortOrder;

    private Integer status;

}
