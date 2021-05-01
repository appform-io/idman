package io.appform.idman.server;

import io.appform.idman.server.db.model.StoredUserRole;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ServiceUserRoleStore {
    boolean mapUserToRole(String userId, String serviceId, String roleId, String assignedBy);
    boolean unmapUserToRole(String userId, String serviceId, String roleId);
    List<StoredUserRole> getUserRoles(String userId);
    Optional<StoredUserRole> getUserServiceRole(String userId, String serviceId);
}
