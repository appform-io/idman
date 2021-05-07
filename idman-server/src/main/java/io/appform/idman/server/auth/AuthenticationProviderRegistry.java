package io.appform.idman.server.auth;

import io.appform.idman.model.AuthMode;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * A registry of auth providers
 */
@Singleton
public class AuthenticationProviderRegistry {
    private final Map<AuthMode, AuthenticationProvider> registry;

    @Inject
    public AuthenticationProviderRegistry(Map<AuthMode, AuthenticationProvider> registry) {
        this.registry = registry;
    }

    public Optional<AuthenticationProvider> provider(AuthMode mode) {
        return Optional.ofNullable(registry.get(mode));
    }

}
