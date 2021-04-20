package io.appform.idman.server.db;

import io.appform.idman.server.db.model.StoredServiceUserRole;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface ServiceUserRoleStore {
    boolean mapUserToRole(String userId, String serviceId, String roleId, String assignedBy);
    boolean unmapUserFromRole(String userId, String serviceId, String roleId);
    List<StoredServiceUserRole> getUserRoles(String userId);
    List<StoredServiceUserRole> getServiceRoleMappings(String serviceId);
    Optional<StoredServiceUserRole> getUserServiceRole(String userId, String serviceId);
}
