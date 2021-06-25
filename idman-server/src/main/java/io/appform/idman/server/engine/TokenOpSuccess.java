package io.appform.idman.server.engine;

import lombok.Value;

/**
 *
 */
@Value
public class TokenOpSuccess implements EngineEvalResult {
    String sessionId;
    String serviceId;
    String userId;

    @Override
    public <T> T accept(EngineEvalResultVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
