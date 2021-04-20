package io.appform.idman.authbundle.security;

import io.appform.idman.model.IdmanUser;
import lombok.Value;

import java.security.Principal;

/**
 * Java principal to be used for the auth system
 */
@Value
public class ServiceUserPrincipal implements Principal {

    IdmanUser serviceUser;

    @Override
    public String getName() {
        return serviceUser.getUser().getId();
    }
}
