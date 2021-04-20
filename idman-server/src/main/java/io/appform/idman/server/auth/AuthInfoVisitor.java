package io.appform.idman.server.auth;

import io.appform.idman.server.auth.impl.GoogleAuthInfo;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;

/**
 *
 */
public interface AuthInfoVisitor<T> {
    T visit(PasswordAuthInfo passwordAuthInfo);

    T visit(GoogleAuthInfo googleAuthInfo);
}
