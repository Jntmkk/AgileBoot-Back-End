package com.agileboot.domain.social.follow.command;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 修改关注UP主命令
 *
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SocialFollowUpUpdateCommand extends SocialFollowUpAddCommand {

    @NotNull
    @Positive
    private Long id;

}
