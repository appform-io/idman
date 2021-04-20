package io.appform.idman.server.auth.configs;

/**
 *
 */
public interface AuthenticationProviderConfigVisitor<T> {
    T visit(CredentialAuthenticationProviderConfig credentialAuthenticationProviderConfig);
    T visit(GoogleAuthenticationProviderConfig googleAuthenticationConfig);
}
