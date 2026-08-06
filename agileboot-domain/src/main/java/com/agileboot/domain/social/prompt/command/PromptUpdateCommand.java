package com.agileboot.domain.social.prompt.command;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PromptUpdateCommand extends PromptAddCommand {

    @NotNull(message = "ID不能为空")
    @Positive
    private Long id;

}
