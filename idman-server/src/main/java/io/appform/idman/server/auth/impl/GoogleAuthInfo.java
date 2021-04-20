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
public class GoogleAuthInfo extends AuthInfo {

    String authToken;

    public GoogleAuthInfo(String authToken) {
        super(AuthMode.GOOGLE_AUTH);
        this.authToken = authToken;
    }

    @Override
    public <T> T accept(AuthInfoVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
