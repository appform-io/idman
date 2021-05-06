package io.appform.idman.server.auth.configs;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.appform.idman.model.AuthMode;
import lombok.Data;

/**
 * Config class for {@link io.appform.idman.server.auth.AuthenticationProvider}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "GOOGLE_AUTH", value = GoogleAuthenticationProviderConfig.class),
        @JsonSubTypes.Type(name = "CREDENTIAL", value = CredentialAuthenticationProviderConfig.class)
})
@Data
public abstract class AuthenticationProviderConfig {
    private final AuthMode type;
    private boolean enabled;

    protected AuthenticationProviderConfig(AuthMode type) {
        this.type = type;
    }

    public abstract <T> T accept(AuthenticationProviderConfigVisitor<T> visitor);
}
