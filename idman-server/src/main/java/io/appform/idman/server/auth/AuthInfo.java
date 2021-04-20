package io.appform.idman.server.auth;

import io.appform.idman.model.AuthMode;
import lombok.Data;

/**
 * Base class for auth info. There would be one sub type for each auth mode
 */
@Data
public abstract class AuthInfo {
    private final AuthMode mode;

    protected AuthInfo(AuthMode mode) {
        this.mode = mode;
    }

    public abstract <T> T accept(final AuthInfoVisitor<T> visitor);
}
