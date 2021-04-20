package io.appform.idman.server;

import io.appform.idman.server.auth.GoogleAuthenticatorContext;
import io.appform.idman.server.auth.PasswordAuthenticatorContext;

/**
 *
 */
public interface AuthenticatorContextVisitor<T> {
    T visit(PasswordAuthenticatorContext passwordAuthenticatorContext);

    T visit(GoogleAuthenticatorContext googleAuthenticatorContext);
}
