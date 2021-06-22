package io.appform.idman.client;

import io.appform.idman.model.IdmanUser;
import lombok.Value;

/**
 *
 */
@Value
public class TokenValidationResult {
    IdmanUser user;
    TokenValidationStatus status;
}
