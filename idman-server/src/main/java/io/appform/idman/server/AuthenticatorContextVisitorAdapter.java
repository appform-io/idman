package io.appform.idman.server;

import io.appform.idman.server.auth.GoogleAuthenticatorContext;
import io.appform.idman.server.auth.PasswordAuthenticatorContext;

/**
 *
 */

public class AuthenticatorContextVisitorAdapter<T> implements AuthenticatorContextVisitor<T> {
    private final T defaultValue;

    public AuthenticatorContextVisitorAdapter() {
        this(null);
    }

    public AuthenticatorContextVisitorAdapter(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public T visit(PasswordAuthenticatorContext passwordAuthenticatorContext) {
        return defaultValue;
    }

    @Override
    public T visit(GoogleAuthenticatorContext googleAuthenticatorContext) {
        return defaultValue;
    }
}
