package io.appform.idman.model;

import lombok.Value;

/**
 *
 */
@Value
public class IdmanUser {
    String sessionId;
    String serviceId;
    User user;
    String role;
}
