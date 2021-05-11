package io.appform.idman.server.auth.configs;

import io.dropwizard.util.Duration;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 *
 */
@Data
public class AuthenticationConfig {

    @NotNull
    @Valid
    private JwtConfig jwt = new JwtConfig();

    @NotNull
    @Valid
    private AuthenticationProviderConfig provider = new CredentialAuthenticationProviderConfig();

    private String domain;

    @NotNull
    @NotEmpty
    private String server;

    @NotNull
    private boolean secureEndpoint;

    @NotNull
    private Duration sessionDuration = Duration.days(30);

}
