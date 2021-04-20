package io.appform.idman.server.auth;

import io.appform.idman.server.AuthenticatorContextVisitor;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;
import lombok.Value;

/**
 *
 */
@Value
public class PasswordAuthenticatorContext implements AuthenticatorContext {
    PasswordAuthInfo pwdAuthInfo;

    @Override
    public <T> T accept(AuthenticatorContextVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
