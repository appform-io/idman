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
import com.fasterxml.jackson.databind.JsonNode;
import io.appform.idman.authcomponents.IdmanAuthDynamicFeature;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.appform.idman.server.engine.Engine;
import io.appform.idman.server.engine.ViewEngineResponseTranslator;
import io.appform.idman.server.utils.TestingUtils;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import javax.ws.rs.core.Form;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.appform.idman.server.utils.TestingUtils.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@Disabled
@Slf4j
class HomeFormValidationsTest {
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
    private final CloseableHttpClient client = HttpClients.custom()
            .disableRedirectHandling()
            .build();

    static {
        doReturn(SharedMetricRegistries.getOrCreate("test")).when(environment).metrics();
//        doReturn(mock(JerseyEnvironment.class)).when(environment).jersey();
    }

    private final ResourceExtension EXT = ResourceExtension.builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(new IdmanAuthDynamicFeature(environment, TestingUtils.clientConfig(), idmanClient))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class))
            .addResource(new Home(new Engine(() -> serviceStore,
                                             () -> roleStore,
                                             () -> userInfoStore,
                                             () -> passwordStore,
                                             () -> userRoleStore), new ViewEngineResponseTranslator()))
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
    @SneakyThrows
    void testCreateServiceAuthCheck() {
        authCheckPost("/ui/services");
    }

    @Test
    void testCreateServiceRoleCheck() {
        roleCheckPost("/ui/services");
    }

    @Test
    void testCreateServiceNoParamValidation() {
        Form form = new Form();
        val errs = validationErrorsPost(
                "/ui/services",
                ImmutableList.<NameValuePair>builder().build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("form field newServiceName must not be null"));
        assertTrue(errs.contains("form field newServiceDescription must not be null"));
        assertTrue(errs.contains("form field newServiceCallbackUrl must not be null"));
    }

    @Test
    void testCreateServiceParamZeroLengthValidation() {
        val errs = validationErrorsPost(
                "/ui/services",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("newServiceName", ""))
                        .add(new BasicNameValuePair("newServiceDescription", ""))
                        .add(new BasicNameValuePair("newServiceCallbackUrl", ""))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("form field newServiceName size must be between 1 and 40"));
        assertTrue(errs.contains("form field newServiceDescription size must be between 1 and 250"));
        assertTrue(errs.contains("form field newServiceCallbackUrl size must be between 1 and 250"));
    }

    @Test
    void testCreateServiceParamHugeLengthValidation() {
        val errs = validationErrorsPost(
                "/ui/services",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("newServiceName", randomString(41)))
                        .add(new BasicNameValuePair("newServiceDescription", randomString(251)))
                        .add(new BasicNameValuePair("newServiceCallbackUrl", randomString(251)))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("form field newServiceName size must be between 1 and 40"));
        assertTrue(errs.contains("form field newServiceDescription size must be between 1 and 250"));
        assertTrue(errs.contains("form field newServiceCallbackUrl size must be between 1 and 250"));
    }

    @Test
    void newServiceAuthCheck() {
        authCheckGet("/ui/services/new");
    }

    @Test
    void newServiceRoleCheck() {
        roleCheckGet("/ui/services/new");
    }

    @Test
    void serviceDetailsAuthCheck() {
        authCheckGet("/ui/services/test");
    }

    @Test
    void serviceDetailsParamLengthCheck() {
        val errs = validationErrorsGet("/ui/services/" + randomString(41));
        assertEquals(1, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 1 and 40"));
    }

    @Test
    void updateServiceDescriptionAuthCheck() {
        authCheckPost("/ui/services/test/update/description");
    }

    @Test
    void updateServiceDescriptionRoleCheck() {
        authCheckPost("/ui/services/test/update/description");
    }

    @Test
    void updateServiceDescriptionParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/test/update/description", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("form field newServiceDescription must not be null"));
    }

    @Test
    void updateServiceDescriptionParamLengthCheck() {
        val errs = validationErrorsPost(
                "/ui/services/" + randomString(41) + "/update/description",
                Collections.singletonList(new BasicNameValuePair("newServiceDescription", randomString(251))));
        assertEquals(2, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("form field newServiceDescription size must be between 1 and 250"));
    }

    @Test
    void updateServiceCallbackUrlAuthCheck() {
        authCheckPost("/ui/services/test/update/callback");
    }

    @Test
    void updateServiceCallbackUrlRoleCheck() {
        authCheckPost("/ui/services/test/update/callback");
    }

    @Test
    void updateServiceCallbackUrlParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/test/update/callback", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("form field newServiceCallbackUrl must not be null"));
    }

    @Test
    void updateServiceCallbackUrlParamLengthCheck() {
        val errs = validationErrorsPost(
                "/ui/services/" + randomString(41) + "/update/callback",
                Collections.singletonList(new BasicNameValuePair("newServiceCallbackUrl", randomString(251))));
        assertEquals(2, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("form field newServiceCallbackUrl size must be between 1 and 250"));
    }

    @Test
    void updateServiceSecretAuthCheck() {
        authCheckPost("/ui/services/test/update/secret");
    }

    @Test
    void updateServiceSecretRoleCheck() {
        authCheckPost("/ui/services/test/update/secret");
    }

    @Test
    void updateServiceSecretParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/" + randomString(41) + "/update/secret", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 1 and 40"));
    }

    @Test
    void deleteServiceAuthCheck() {
        authCheckPost("/ui/services/test/delete");
    }

    @Test
    void deleteServiceRoleCheck() {
        authCheckPost("/ui/services/test/delete");
    }

    @Test
    void deleteServiceParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/" + randomString(41) + "/delete", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 1 and 40"));
    }

    @Test
    void createRoleAuthCheck() {
        authCheckPost("/ui/services/test/roles");
    }

    @Test
    void createRoleRoleCheck() {
        authCheckPost("/ui/services/test/roles");
    }


    @Test
    void createRoleParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/test/roles", Collections.emptyList());
        assertEquals(2, errs.size());
        assertTrue(errs.contains("form field newRoleName must not be empty"));
        assertTrue(errs.contains("form field newRoleDescription must not be empty"));
    }

    @Test
    void createRoleParamLengthCheck() {
        val errs = validationErrorsPost(
                "/ui/services/" + randomString(41) + "/roles",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("newRoleName", randomString(41)))
                        .add(new BasicNameValuePair("newRoleDescription", randomString(251)))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("form field newRoleName size must be between 0 and 40"));
        assertTrue(errs.contains("form field newRoleDescription size must be between 0 and 250"));
    }

    @Test
    void updateRoleAuthCheck() {
        authCheckPost("/ui/services/test/roles/r1/update");
    }

    @Test
    void updateRoleRoleCheck() {
        authCheckPost("/ui/services/test/roles/r1/update");
    }

    @Test
    void updateRoleParamMissingCheck() {
        val errs = validationErrorsPost("/ui/services/test/roles/r1/update", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("form field roleDescription must not be empty"));
    }

    @Test
    void updateRoleParamLengthCheck() {
        val errs = validationErrorsPost(
                "/ui/services/" + randomString(41) + "/roles/" + randomString(41) + "/update",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("roleDescription", randomString(251)))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("path param roleId size must be between 0 and 40"));
        assertTrue(errs.contains("form field roleDescription size must be between 0 and 250"));
    }

    @Test
    void deleteRoleAuthCheck() {
        authCheckPost("/ui/services/test/roles/r1/delete");
    }

    @Test
    void deleteRoleRoleCheck() {
        authCheckPost("/ui/services/test/roles/r1/delete");
    }

    @Test
    void deleteRoleParamLengthCheck() {
        val errs = validationErrorsPost(
                "/ui/services/" + randomString(41) + "/roles/" + randomString(41) + "/delete",
                Collections.emptyList());
        assertEquals(2, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("path param roleId size must be between 0 and 40"));
    }

    @Test
    void createUserAuthCheck() {
        authCheckGet("/ui/users/new");
    }

    @Test
    void createUserRoleCheck() {
        authCheckGet("/ui/users/new");
    }

    @Test
    void createHumanUserAuthCheck() {
        authCheckGet("/ui/users/human");
    }

    @Test
    void createHumanUserRoleCheck() {
        authCheckGet("/ui/users/human");
    }

    @Test
    void createHumanUserMissingParamsCheck() {
        val errs = validationErrorsPost("/ui/users/human", Collections.emptyList());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("form field email must not be empty"));
        assertTrue(errs.contains("form field name must not be empty"));
        assertTrue(errs.contains("form field password must not be empty"));
    }

    @Test
    void createHumanUserHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/human",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("email", randomString(251)))
                        .add(new BasicNameValuePair("name", randomString(251)))
                        .add(new BasicNameValuePair("password", randomString(41)))
                        .build());
        assertEquals(4, errs.size());
        assertTrue(errs.contains("form field email must be a well-formed email address"));
        assertTrue(errs.contains("form field email size must be between 0 and 250"));
        assertTrue(errs.contains("form field name size must be between 0 and 250"));
        assertTrue(errs.contains("form field password size must be between 0 and 40"));
    }

    @Test
    void userDetailsAuthCheck() {
        authCheckGet("/ui/users/abc");
    }


    @Test
    void updateUserAuthCheck() {
        authCheckPost("/ui/users/abc/update");
    }

    @Test
    void updateUserMissingParamsCheck() {
        val errs = validationErrorsPost("/ui/users/abc/update", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("form field name must not be empty"));
    }

    @Test
    void updateUserHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/" + randomString(41) + "/update",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("name", randomString(251)))
                        .build());
        assertEquals(2, errs.size());
        assertTrue(errs.contains("path param userId size must be between 0 and 40"));
        assertTrue(errs.contains("form field name size must be between 0 and 250"));
    }

    @Test
    void deleteUserAuthCheck() {
        authCheckPost("/ui/users/abc/delete");
    }

    @Test
    void deleteUserRoleCheck() {
        roleCheckPost("/ui/users/abc/delete");
    }

    @Test
    void deleteUserHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/" + randomString(41) + "/delete", Collections.emptyList());
        assertEquals(1, errs.size());
        assertTrue(errs.contains("path param userId size must be between 0 and 40"));
    }

    @Test
    void changePasswordAuthCheck() {
        authCheckPost("/ui/users/abc/update/password");
    }

    @Test
    void changePasswordHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/" + randomString(41) + "/update/password",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("oldPassword", randomString(41)))
                        .add(new BasicNameValuePair("newPassword", randomString(41)))
                        .add(new BasicNameValuePair("newPasswordConf", randomString(41)))
                        .build());
        assertEquals(4, errs.size());
        assertTrue(errs.contains("path param userId size must be between 0 and 40"));
        assertTrue(errs.contains("form field oldPassword size must be between 0 and 40"));
        assertTrue(errs.contains("form field newPassword size must be between 0 and 40"));
        assertTrue(errs.contains("form field newPasswordConf size must be between 0 and 40"));
    }

    @Test
    void changePasswordMissingParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/abc/update/password", Collections.emptyList());
        assertEquals(3, errs.size());

        assertTrue(errs.contains("form field oldPassword must not be empty"));
        assertTrue(errs.contains("form field newPassword must not be empty"));
        assertTrue(errs.contains("form field newPasswordConf must not be empty"));
    }

    @Test
    void changePasswordForcedAuthCheck() {
        authCheckPost("/ui/users/abc/update/password/forced");
    }

    @Test
    void changePasswordForcedRoleCheck() {
        roleCheckPost("/ui/users/abc/update/password/forced");
    }

    @Test
    void changePasswordForcedHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/" + randomString(41) + "/update/password/forced",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("newPassword", randomString(41)))
                        .add(new BasicNameValuePair("newPasswordConf", randomString(41)))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("path param userId size must be between 0 and 40"));
        assertTrue(errs.contains("form field newPassword size must be between 0 and 40"));
        assertTrue(errs.contains("form field newPasswordConf size must be between 0 and 40"));
    }

    @Test
    void changePasswordForcedMissingParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/users/abc/update/password/forced", Collections.emptyList());
        assertEquals(2, errs.size());

        assertTrue(errs.contains("form field newPassword must not be empty"));
        assertTrue(errs.contains("form field newPasswordConf must not be empty"));
    }

    @Test
    void mapUserToRoleAuthCheck() {
        authCheckPost("/ui/roles/abc/map");
    }

    @Test
    void mapUserToRoleForcedRoleCheck() {
        roleCheckPost("/ui/roles/abc/map");
    }

    @Test
    void mapUserToRoleHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/roles/" + randomString(41) + "/map",
                ImmutableList.<NameValuePair>builder()
                        .add(new BasicNameValuePair("roleId", randomString(41)))
                        .add(new BasicNameValuePair("userId", randomString(41)))
                        .build());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("form field roleId size must be between 0 and 40"));
        assertTrue(errs.contains("form field userId size must be between 0 and 40"));
    }

    @Test
    void mapUserToRoleMissingParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/roles/abc/map", Collections.emptyList());
        assertEquals(2, errs.size());

        assertTrue(errs.contains("form field roleId must not be empty"));
        assertTrue(errs.contains("form field userId must not be empty"));
    }

    @Test
    void unmapUserToRoleAuthCheck() {
        authCheckPost("/ui/roles/abc/def/unmap/xyx");
    }

    @Test
    void unmapUserToRoleForcedRoleCheck() {
        roleCheckPost("/ui/roles/abc/def/unmap/xyx");
    }

    @Test
    void unmapUserToRoleHugeParamsCheck() {
        val errs = validationErrorsPost(
                "/ui/roles/" + randomString(41) + "/" + randomString(41) + "/unmap/" + randomString(41),
                Collections.emptyList());
        assertEquals(3, errs.size());
        assertTrue(errs.contains("path param serviceId size must be between 0 and 40"));
        assertTrue(errs.contains("path param roleId size must be between 0 and 40"));
        assertTrue(errs.contains("path param userId size must be between 0 and 40"));
    }

    @SneakyThrows
    private Set<String> validationErrorsPost(String path, List<NameValuePair> params) {
        val post = new HttpPost(EXT.target(path).getUri());
        post.setEntity(new UrlEncodedFormEntity(params));
        return validationErrorExec(path, post);
    }

    @SneakyThrows
    private Set<String> validationErrorsGet(String path) {
        val post = new HttpGet(EXT.target(path).getUri());
        return validationErrorExec(path, post);
    }

    private Set<String> validationErrorExec(String path, HttpUriRequest request) throws IOException {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ADMIN_TOKEN");
        try (val r = client.execute(request)) {
            val node = EXT.getObjectMapper().readTree(EntityUtils.toByteArray(r.getEntity()));
            val errs = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(node.get("errors").iterator(), Spliterator.ORDERED), false)
                    .map(JsonNode::asText)
                    .collect(Collectors.toSet());
            log.error("Errors from {}: {}", path, errs);
            return errs;
        }
    }

    @SneakyThrows
    private void authCheckGet(String path) {
        HttpGet get = new HttpGet(EXT.target(path).getUri());
        authCheck(get);
    }

    @SneakyThrows
    private void authCheckPost(String path) {
        HttpPost post = new HttpPost(EXT.target(path).getUri());
        post.setEntity(new UrlEncodedFormEntity(new ArrayList<>()));
        authCheck(post);
    }

    @SneakyThrows
    private void authCheck(HttpUriRequest request) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer WRONG_TOKEN");

        try (val r = client.execute(request)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            assertEquals("/apis/idman/auth",
                         URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue()).getPath());
        }
    }

    @SneakyThrows
    private void roleCheckGet(final String path) {
        HttpGet get = new HttpGet(EXT.target(path).getUri());
        roleCheck(get);
    }

    @SneakyThrows
    private void roleCheckPost(final String path) {
        HttpPost post = new HttpPost(EXT.target(path).getUri());
        post.setEntity(new UrlEncodedFormEntity(new ArrayList<>()));
        roleCheck(post);
    }

    @SneakyThrows
    private void roleCheck(HttpUriRequest request) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer USER_TOKEN");

        try (val r = client.execute(request)) {
            assertEquals(HttpStatus.SC_FORBIDDEN, r.getStatusLine().getStatusCode());
        }
    }
}