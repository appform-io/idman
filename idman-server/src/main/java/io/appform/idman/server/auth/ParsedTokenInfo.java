package io.appform.idman.server.auth;

import io.appform.idman.model.TokenType;
import lombok.Value;
import org.jose4j.jwt.NumericDate;

/**
 *
 */
@Value
public class ParsedTokenInfo {
    String userId;
    String sessionId;
    String serviceId;
    NumericDate expiry;
    TokenType type;
}
