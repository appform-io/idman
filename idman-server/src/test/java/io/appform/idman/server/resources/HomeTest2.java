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
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import io.appform.idman.server.Engine;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.appform.idman.server.utils.TestingUtils;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.val;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.vyarus.guicey.gsp.app.rest.support.TemplateAnnotationFilter;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class HomeTest2 {
    private static final User ADMIN_USER = new User("TA1", "TA", UserType.HUMAN, AuthMode.PASSWORD);
    private static final User NORMAL_USER = new User("TU1", "TU", UserType.HUMAN, AuthMode.PASSWORD);
    private static final IdmanUser TEST_ADMIN = new IdmanUser("SS1", "S1", ADMIN_USER, "IDMAN_ADMIN");

    private static final IdmanUser TEST_USER = new IdmanUser("SS1", "S1", NORMAL_USER, "IDMAN_USER");

    private final ServiceStore serviceStore = mock(ServiceStore.class);
    private final RoleStore roleStore = mock(RoleStore.class);
    private final UserInfoStore userInfoStore = mock(UserInfoStore.class);
    private final PasswordStore passwordStore = mock(PasswordStore.class);
    private final UserRoleStore userRoleStore = mock(UserRoleStore.class);
    private static final Environment environment = mock(Environment.class);
    private final IdManClient idmanClient = mock(IdManClient.class);

    static {
        doReturn(SharedMetricRegistries.getOrCreate("test")).when(environment).metrics();
//        doReturn(mock(JerseyEnvironment.class)).when(environment).jersey();
    }

    private final ResourceExtension EXT = ResourceExtension.builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(new IdmanAuthDynamicFeature(environment, TestingUtils.clientConfig(), idmanClient))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class))
            .addProvider(new TemplateAnnotationFilter())
//            .addResource(new Hello())
            .addResource(new Home(() -> serviceStore,
                                  () -> roleStore,
                                  () -> userInfoStore,
                                  () -> passwordStore,
                                  () -> userRoleStore,
                                  new Engine(null, null, null, null, null)))
            .build();

    @BeforeEach
    void setup() {
        doReturn(Optional.of(TEST_ADMIN)).when(idmanClient).validate(eq("ADMIN_TOKEN"), anyString());
        doReturn(Optional.of(TEST_USER)).when(idmanClient).validate(eq("USER_TOKEN"), anyString());
        doReturn(Optional.empty()).when(idmanClient).validate(eq("WRONG_TOKEN"), anyString());

        val admin = new StoredUser("TA1", "ta@t.com", "TA", UserType.HUMAN);
        admin.setAuthState(new StoredUserAuthState(AuthMode.PASSWORD, AuthState.ACTIVE, 0, admin));

        val user = new StoredUser("TU1", "tu@t.com", "TU", UserType.HUMAN);
        user.setAuthState(new StoredUserAuthState(AuthMode.PASSWORD, AuthState.ACTIVE, 0, user));

        doReturn(Optional.of(admin)).when(userInfoStore).get("TA1");
        doReturn(Optional.of(user)).when(userInfoStore).get("TU1");
    }

    @AfterEach
    void destroy() {
        reset(idmanClient);
    }

    @Test
    void testRun() {
        assertEquals("hello TU",
                     EXT.target("/ui")
                             .request()
                             .header(HttpHeaders.AUTHORIZATION, "Bearer USER_TOKEN")
                             .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
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