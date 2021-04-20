package io.appform.idman.server.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.idman.server.AuthenticatorContextVisitor;
import lombok.Data;

/**
 *
 */
@Data
public class GoogleAuthenticatorContext implements AuthenticatorContext {

    private final String authToken;
    private JsonNode identity;
    private String email;

    public GoogleAuthenticatorContext(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public <T> T accept(AuthenticatorContextVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
