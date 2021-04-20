package io.appform.idman.server.auth.configs;

import io.appform.idman.model.AuthMode;
import io.dropwizard.util.Duration;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.Proxy;

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
    private AuthMode mode;

    @NotNull
    @Valid
    private GoogleAuthenticationProviderConfig provider = new GoogleAuthenticationProviderConfig();

    private String domain;

    @NotNull
    @NotEmpty
    private String server;

    @NotNull
    private boolean secureEndpoint;

    private Proxy.Type proxyType;

    private String proxyHost;

    private int proxyPort;

    @NotNull
    private Duration sessionDuration = Duration.days(30);

}
