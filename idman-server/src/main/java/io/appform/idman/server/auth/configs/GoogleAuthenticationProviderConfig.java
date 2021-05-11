package io.appform.idman.server.auth.configs;

import io.appform.idman.model.AuthMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.net.Proxy;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GoogleAuthenticationProviderConfig extends AuthenticationProviderConfig {
    @NotEmpty
    private String clientId;

    @NotEmpty
    private String clientSecret;

    private String loginDomain;

    private Proxy.Type proxyType;

    private String proxyHost;

    private int proxyPort;

    public GoogleAuthenticationProviderConfig() {
        super(AuthMode.GOOGLE_AUTH);
    }

    @Override
    public <T> T accept(AuthenticationProviderConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
