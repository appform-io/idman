package io.appform.idman.authcomponents.security;

import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.client.IdManClient;
import io.dropwizard.auth.Authenticator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Authenticates token by calling server.
 */
@Slf4j
@Singleton
public class IdmanAuthenticator implements Authenticator<String, ServiceUserPrincipal> {

    private final IdmanClientConfig config;
    private final IdManClient idManClient;

    @Inject
    public IdmanAuthenticator(IdmanClientConfig config, IdManClient idManClient) {
        this.config = config;
        this.idManClient = idManClient;
    }

    @Override
    public Optional<ServiceUserPrincipal> authenticate(String token) {
        val serviceSessionUser = idManClient.validate(token, config.getServiceId()).orElse(null);
        if (serviceSessionUser == null) {
            log.warn("authentication_failed::invalid_session token:{}", token);
            return Optional.empty();
        }
        log.debug("authentication_success userId:{} tokenId:{}",
                  serviceSessionUser.getUser().getId(), serviceSessionUser.getSessionId());
        return Optional.of(new ServiceUserPrincipal(serviceSessionUser));
    }
}
