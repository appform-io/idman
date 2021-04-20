package io.appform.idman.authbundle;

import io.appform.idman.authbundle.security.ServiceUserPrincipal;

import java.util.Optional;

/**
 *
 */
public interface DefaultHandler {
    Optional<ServiceUserPrincipal> defaultUser();
}
