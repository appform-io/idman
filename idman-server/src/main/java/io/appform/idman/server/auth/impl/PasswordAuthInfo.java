package io.appform.idman.server.auth.impl;

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.AuthInfo;
import io.appform.idman.server.auth.AuthInfoVisitor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordAuthInfo extends AuthInfo {

    String email;
    String password;

    public PasswordAuthInfo(String email, String password, String serviceId, String clientSessionId) {
        super(AuthMode.PASSWORD, serviceId, clientSessionId);
        this.email = email;
        this.password = password;
    }

    @Override
    public <T> T accept(AuthInfoVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
