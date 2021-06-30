package io.appform.idman.server.engine.results;

import io.appform.idman.server.engine.EngineEvalResult;
import io.appform.idman.server.engine.EngineEvalResultVisitor;
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
