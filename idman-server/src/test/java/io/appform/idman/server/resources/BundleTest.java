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

package io.appform.idman.server.resources;

import com.codahale.metrics.SharedMetricRegistries;
import io.appform.idman.authcomponents.IdmanAuthDynamicFeature;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.ClientTestingUtils;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@Disabled
class BundleTest {
    private static final IdmanUser TEST_ADMIN = new IdmanUser("SS1",
                                                              "S1",
                                                              new User("t@t.com",
                                                                       "TA",
                                                                       UserType.HUMAN,
                                                                       AuthMode.PASSWORD),
                                                              "IDMAN_ADMIN");
    private static final IdmanUser TEST_USER = new IdmanUser("SS1",
                                                             "S1",
                                                             new User("t@t.com",
                                                                      "TU",
                                                                      UserType.HUMAN,
                                                                      AuthMode.PASSWORD),
                                                             "IDMAN_USER");

    private static final Environment environment = mock(Environment.class);
    private final IdManClient idmanClient = mock(IdManClient.class);

    static {
        doReturn(SharedMetricRegistries.getOrCreate("test")).when(environment).metrics();
    }

    private final ResourceExtension EXT = ResourceExtension.builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(new IdmanAuthDynamicFeature(environment, ClientTestingUtils.clientConfig(), idmanClient))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class))
            .addResource(new Hello())
            .build();

    @BeforeEach
    void setup() {
        doReturn(Optional.of(TEST_ADMIN)).when(idmanClient).validate(eq("ADMIN_TOKEN"), anyString());
        doReturn(Optional.of(TEST_USER)).when(idmanClient).validate(eq("USER_TOKEN"), anyString());
        doReturn(Optional.empty()).when(idmanClient).validate(eq("WRONG_TOKEN"), anyString());
    }

    @AfterEach
    void destroy() {
        reset(idmanClient);
    }

    @Test
    void testRun() {
        assertEquals("hello TU",
                     EXT.target("/hello")
                             .request()
                             .header(HttpHeaders.AUTHORIZATION, "Bearer USER_TOKEN")
                             .get(String.class));
        assertEquals("hello TA",
                     EXT.target("/hello")
                             .request()
                             .header(HttpHeaders.AUTHORIZATION, "Bearer ADMIN_TOKEN")
                             .get(String.class));
        assertEquals(HttpStatus.SC_NOT_FOUND,
                     EXT.target("/hello")
                             .request()
                             .header(HttpHeaders.AUTHORIZATION, "Bearer WRONG_TOKEN")
                             .get()
                             .getStatus());
    }
}
