package io.appform.idman.server.db;

import io.appform.idman.server.db.model.StoredService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ServiceStore {
    Optional<StoredService> create(String name, String description, String callbackUrl);
    Optional<StoredService> get(String serviceId);
    Optional<StoredService> updateDescription(String serviceId, String description);
    Optional<StoredService> updateCallbackUrl(String serviceId, String callbackUrl);
    Optional<StoredService> updateSecret(String serviceId);
    boolean delete(String serviceId);

    List<StoredService> get(Collection<String> serviceIds);

    List<StoredService> list(boolean includeDeleted);
}
