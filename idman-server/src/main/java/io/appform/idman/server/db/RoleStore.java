package io.appform.idman.server.db;

import io.appform.idman.server.db.model.StoredRole;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface RoleStore {
    Optional<StoredRole> create(String serviceId, String displayName, String description);

    Optional<StoredRole> get(String serviceId, String roleId);
    Optional<StoredRole> update(String serviceId, String roleId, String description);
    boolean delete(String serviceId, String roleId);
    List<StoredRole> list(String serviceId, boolean includeDeleted);
    List<StoredRole> get(Collection<String> roleIds);
}
