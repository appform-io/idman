package io.appform.idman.server.db;

import io.appform.idman.server.db.model.StoredService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ServiceStore {
    Optional<StoredService> create(String name, String description);
    Optional<StoredService> get(String serviceId);
    Optional<StoredService> update(String serviceId, String description);
    boolean delete(String serviceId);

    List<StoredService> get(Collection<String> serviceIds);

    List<StoredService> list(boolean includeDeleted);
}
