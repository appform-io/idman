package io.appform.idman.authcomponents;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.appform.idman.authcomponents.filters.IdmanAuthFilter;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.authcomponents.security.IdmanAuthenticator;
import io.appform.idman.authcomponents.security.IdmanRoleAuthorizer;
import io.appform.idman.authcomponents.security.RedirectUnauthorizedHandler;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthorizer;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

/**
 *
 */
@Provider
@Slf4j
public class IdmanDynamicFeature extends AuthDynamicFeature {
    @Inject
    public IdmanDynamicFeature(
            Environment environment,
            IdmanClientConfig authConfig,
            IdManClient idManClient) {
        super(new IdmanAuthFilter.Builder(authConfig)
                .setAuthenticator(new IdmanAuthenticator(authConfig, idManClient))
                .setAuthorizer(new CachingAuthorizer<>(environment.metrics(),
                                                       new IdmanRoleAuthorizer(),
                                                       CaffeineSpec.parse(authConfig.getCacheSpec())))
                .setUnauthorizedHandler(new RedirectUnauthorizedHandler(authConfig))
             .buildAuthFilter());
        environment.jersey().register(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class));
        environment.jersey().register(RolesAllowedDynamicFeature.class);
//        environment.jersey().register(new IdmanAuthHandler(idManClient, authConfig));
        log.info("IDMan dynamic feature enabled");
    }
}
