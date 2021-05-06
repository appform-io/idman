package io.appform.idman.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.idman.server.auth.configs.*;
import io.appform.idman.server.auth.impl.GoogleAuthenticationProvider;
import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.auth.impl.PasswordAuthenticationProvider;

import javax.inject.Provider;

/**
 * Creates auth provider based on config
 */
public class AuthenticationProviderFactory implements AuthenticationProviderConfigVisitor<AuthenticationProvider> {

    private final AuthenticationConfig authConfig;
    private final ObjectMapper mapper;
    private final Provider<UserInfoStore> userStore;
    private final Provider<SessionStore> sessionStore;
    private final Provider<PasswordStore> passwordStore;

    public AuthenticationProviderFactory(
            AuthenticationConfig authConfig,
            ObjectMapper mapper,
            Provider<UserInfoStore> userStore,
            Provider<SessionStore> sessionStore,
            Provider<PasswordStore> passwordStore) {
        this.authConfig = authConfig;
        this.mapper = mapper;
        this.userStore = userStore;
        this.sessionStore = sessionStore;
        this.passwordStore = passwordStore;
    }

    public AuthenticationProvider create(AuthenticationProviderConfig providerConfig) {
        return providerConfig.accept(this);
    }

    @Override
    public AuthenticationProvider visit(CredentialAuthenticationProviderConfig credentialAuthenticationProviderConfig) {
        return new PasswordAuthenticationProvider(authConfig, userStore, passwordStore, sessionStore);
    }

    @Override
    public AuthenticationProvider visit(GoogleAuthenticationProviderConfig googleAuthenticationConfig) {
        return new GoogleAuthenticationProvider(
                authConfig, googleAuthenticationConfig, mapper, userStore, sessionStore);
    }
}
