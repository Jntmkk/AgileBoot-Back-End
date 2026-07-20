package com.agileboot.domain.social.account.command;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author SocialMedia-Hub
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SocialAccountUpdateCommand extends SocialAccountAddCommand {

    @NotNull
    @Positive
    private Long id;

}
