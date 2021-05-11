package io.appform.idman.authcomponents.security;

import io.dropwizard.auth.Authorizer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Singleton;

/**
 *
 */
@Singleton
@Slf4j
public class IdmanRoleAuthorizer implements Authorizer<ServiceUserPrincipal> {
    @Override
    @SuppressWarnings("deprecation")
    public boolean authorize(ServiceUserPrincipal userPrincipal, String role) {
        val user = userPrincipal.getServiceUser();

        if(!user.getRole().equals(role)) {
            log.warn("User {} is trying to access unauthorized role: {}", user.getUser().getId(), role);
            return false;
        }
        return true;
    }
}
