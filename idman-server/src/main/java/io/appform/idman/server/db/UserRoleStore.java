package io.appform.idman.server.db;

import io.appform.idman.server.db.model.StoredUserRole;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface UserRoleStore {
    void mapUserToRole(String userId, String serviceId, String roleId, String assignedBy);
    boolean unmapUserFromRole(String userId, String serviceId);
    List<StoredUserRole> getUserRoles(String userId);
    List<StoredUserRole> getServiceRoleMappings(String serviceId);
    Optional<StoredUserRole> getUserServiceRole(String userId, String serviceId);
}
