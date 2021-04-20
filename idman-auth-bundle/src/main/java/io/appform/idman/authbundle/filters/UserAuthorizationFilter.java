package io.appform.idman.authbundle.filters;


import io.appform.idman.authbundle.DefaultHandler;
import io.appform.idman.authbundle.IdmanAuthenticationConfig;
import io.appform.idman.authbundle.security.ServiceUserPrincipal;
import io.appform.idman.authbundle.SessionUser;
import io.dropwizard.auth.Authorizer;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

/**
 * This filter assigns role to validated user
 */
@Priority(Priorities.AUTHENTICATION)
public class UserAuthorizationFilter implements ContainerRequestFilter {

    private final IdmanAuthenticationConfig authConfig;
    private final Authorizer<ServiceUserPrincipal> authorizer;
    private final DefaultHandler defaultHandler;

    public UserAuthorizationFilter(
            IdmanAuthenticationConfig authConfig,
            Authorizer<ServiceUserPrincipal> authorizer,
            DefaultHandler defaultHandler) {
        this.authConfig = authConfig;
        this.authorizer = authorizer;
        this.defaultHandler = defaultHandler;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if(!authConfig.isEnabled()) {
//            updateContext(requestContext, defaultHandler.defaultUser().orElse(null));
            return;
        }
        ServiceUserPrincipal principal = SessionUser.take();
        if(null != principal) {
            updateContext(requestContext, principal);
            return;
        }
        throw new IllegalStateException("No valid session user found");
    }

    private void updateContext(ContainerRequestContext requestContext, ServiceUserPrincipal principal) {
        requestContext.setSecurityContext(new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return principal;
            }

            @Override
            public boolean isUserInRole(String role) {
                return authorizer.authorize(principal, role, requestContext);
            }

            @Override
            public boolean isSecure() {
                return requestContext.getSecurityContext().isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return SecurityContext.BASIC_AUTH;
            }

        });
    }
}
