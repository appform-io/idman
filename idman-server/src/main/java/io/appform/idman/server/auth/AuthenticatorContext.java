package io.appform.idman.server.auth;

import io.appform.idman.server.AuthenticatorContextVisitor;

/**
 *
 */
public interface AuthenticatorContext {
    <T> T accept(final AuthenticatorContextVisitor<T> visitor);
}
