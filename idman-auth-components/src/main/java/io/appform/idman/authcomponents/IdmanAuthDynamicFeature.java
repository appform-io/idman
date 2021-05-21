/*
 * Copyright 2021. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.idman.authcomponents;

import com.github.benmanes.caffeine.cache.CaffeineSpec;
import io.appform.idman.authcomponents.filters.IdmanAuthFilter;
import io.appform.idman.authcomponents.security.IdmanAuthenticator;
import io.appform.idman.authcomponents.security.IdmanRoleAuthorizer;
import io.appform.idman.authcomponents.security.RedirectUnauthorizedHandler;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.CachingAuthorizer;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

/**
 *
 */
@Provider
@Slf4j
public class IdmanAuthDynamicFeature extends AuthDynamicFeature {
    @Inject
    public IdmanAuthDynamicFeature(
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
        val jersey = environment.jersey();
        if(null != jersey) { //Will happen during testing
            jersey.register(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class));
            jersey.register(RolesAllowedDynamicFeature.class);
        }
        else {
            log.warn("No jersey container. Skipped adding stuff");
        }
        log.info("IDMan dynamic feature enabled");
    }
}
