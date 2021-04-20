package io.appform.idman.model;

import lombok.Value;

/**
 *
 */
@Value
public class IdmanUser {
    String sessionId;
    User user;
    String role;
}
