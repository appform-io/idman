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

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.localauth.LocalIdmanAuthClient;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;

import static io.appform.idman.client.ClientTestingUtils.tokenInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class ApisTest {

    private static final ServiceStore serviceStore = mock(ServiceStore.class);
    private static final LocalIdmanAuthClient client = mock(LocalIdmanAuthClient.class);
    private static final ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new Apis(() -> serviceStore, () -> client))
            .build();


    @BeforeEach
    void setup() {

    }

    @AfterEach
    void teardown() {
        reset(serviceStore);
        reset(client);
    }

    @Test
    @SneakyThrows
    void testNoAuth() {
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testNoToken() {
        val form = new Form();
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testNoService() {
        val form = new Form();
        val response = EXT.target("/auth/check/v1/")
                .request()
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testInvalidService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer B")
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testMalformedToken() {
        val service = new StoredService("S1", "Test Service", "", "blah.com", "SECRET_TOKEN");
        doReturn(Optional.of(service)).when(serviceStore).get("S1");
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer")
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testCorrectToken() {
        val service = new StoredService("S1", "Test Service", "", "blah.com", "SECRET_TOKEN");
        doReturn(Optional.of(service)).when(serviceStore).get("S1");

        val idmanUser = new IdmanUser("S1", service.getServiceId(), new User("U1", "TU", UserType.HUMAN, AuthMode.PASSWORD), "R");
        val tokenInfo =
        doReturn(Optional.of(tokenInfo("T", idmanUser)))
                .when(client)
                .refreshAccessToken(service.getServiceId(), "T");
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer SECRET_TOKEN")
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testWrongToken() {
        val service = new StoredService("S1", "Test Service", "", "blah.com", "SECRET_TOKEN");
        doReturn(Optional.of(service)).when(serviceStore).get("S1");

        val idmanUser = new IdmanUser("S1", service.getServiceId(), new User("U1", "TU", UserType.HUMAN, AuthMode.PASSWORD), "R");
        doReturn(Optional.of(idmanUser))
                .when(client)
                .refreshAccessToken(service.getServiceId(), "T");
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer T")
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    @SneakyThrows
    void testWrongData() {
        val service = new StoredService("S1", "Test Service", "", "blah.com", "SECRET_TOKEN");
        doReturn(Optional.of(service)).when(serviceStore).get("S1");

        doReturn(Optional.empty())
                .when(client)
                .refreshAccessToken(anyString(), anyString());
        val form = new Form();
        form.param("token", "T");
        val response = EXT.target("/auth/check/v1/S1")
                .request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer SECRET_TOKEN")
                .buildPost(Entity.form(form))
                .invoke();
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    }
}