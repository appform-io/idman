package io.appform.idman.server.auth;

import io.appform.idman.model.AuthMode;
import lombok.Data;

/**
 * Base class for auth info. There would be one sub type for each auth mode
 */
@Data
public abstract class AuthInfo {
    private final AuthMode mode;
    private final String serviceId;
    private final String clientSessionId;

    protected AuthInfo(AuthMode mode, String serviceId, String clientSessionId) {
        this.mode = mode;
        this.serviceId = serviceId;
        this.clientSessionId = clientSessionId;
    }

    public abstract <T> T accept(final AuthInfoVisitor<T> visitor);
}
