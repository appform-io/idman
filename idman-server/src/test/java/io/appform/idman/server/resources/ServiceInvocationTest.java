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

import io.appform.idman.server.App;
import io.appform.idman.server.AppConfig;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@Testcontainers
class ServiceInvocationTest {

    @Container
    private static final MariaDBContainer mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.5"))
            .withInitScript("01-init.sql")
            .withDatabaseName("idman_db")
            .withUsername("db_user")
            .withPassword("db_password")
            .waitingFor(Wait.forLogMessage("Executed database script from 01-init.sql", 1));

    private static DropwizardAppExtension<AppConfig> EXT;

    static {
        mariaDB.start();
        EXT = new DropwizardAppExtension<>(
                App.class,
                ResourceHelpers.resourceFilePath("test-app-config.yaml"),
                ConfigOverride.config("db.url", mariaDB.getJdbcUrl()),
                ConfigOverride.config("db.user", mariaDB.getUsername()),
                ConfigOverride.config("db.password", mariaDB.getPassword()));
    }

    @Test
    @SneakyThrows
    void testRun() {
        final CloseableHttpClient client = HttpClients.custom()
                .disableRedirectHandling()
                .build();
        val get = new HttpGet(String.format("http://localhost:%d", EXT.getLocalPort()));
        get.addHeader(HttpHeaders.AUTHORIZATION, "Bearer USER_TOKEN");
        try (final CloseableHttpResponse response = client.execute(get)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, response.getStatusLine().getStatusCode());
        }
        catch (Exception e) {
            fail();
        }
        client.close();
    }
}