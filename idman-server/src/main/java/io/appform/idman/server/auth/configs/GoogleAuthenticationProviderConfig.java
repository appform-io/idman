package io.appform.idman.server.auth.configs;

import io.appform.idman.model.AuthMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;

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

    public GoogleAuthenticationProviderConfig() {
        super(AuthMode.GOOGLE_AUTH);
    }

    @Override
    public <T> T accept(AuthenticationProviderConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
