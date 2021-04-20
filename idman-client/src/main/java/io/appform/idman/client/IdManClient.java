package io.appform.idman.client;

import io.appform.idman.model.IdmanUser;

import java.util.Optional;

/**
 * Abstraction for client information
 */
public interface IdManClient {
    Optional<IdmanUser> validate(String serviceId, String token);
    Optional<IdmanUser> getUserInfo(String serviceId, String userId);
}
