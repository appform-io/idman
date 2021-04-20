package io.appform.idman.server.auth.configs;

import io.appform.idman.model.AuthMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CredentialAuthenticationProviderConfig extends AuthenticationProviderConfig {
    public CredentialAuthenticationProviderConfig() {
        super(AuthMode.PASSWORD);
    }

    @Override
    public <T> T accept(AuthenticationProviderConfigVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
