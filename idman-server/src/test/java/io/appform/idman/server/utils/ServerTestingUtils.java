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

package io.appform.idman.server.utils;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.TokenType;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.AuthState;
import io.appform.idman.server.db.model.ClientSession;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.dropwizard.util.Duration;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import ru.vyarus.guicey.gsp.views.template.TemplateContext;

import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 *
 */
@UtilityClass
public class ServerTestingUtils {
    public static AuthenticationConfig passwordauthConfig() {
        val authenticationConfig = new AuthenticationConfig();
        authenticationConfig.setDomain("testd");
        authenticationConfig.setServer("http://localhost:8080");
        authenticationConfig.setSessionDuration(Duration.days(7));

        val jwtConfig = new JwtConfig();
        jwtConfig.setIssuerId("testissuer");
        jwtConfig.setPrivateKey(
                "bYdNUUyCqx8IuGNqhFYS27WizZrfupAmJS8I4mfj2Cjox9Nc04Oews9tJEiDTrJfopzKdjygi8SgXeopSe/rPYqEKfrAUw/Dn6wMVhE56S7/5DKAvYusA2fQRqxOrOosO1lERnArw15tkAf/z5QUUUXnKZZTiczNEebjs2OG5s94PGxtQzxtYsZ1q2oXoq4lKPTosPpwkRxeh8LQCweDGR80xgoM1+yDAoYIeg==");
        authenticationConfig.setJwt(jwtConfig);
        return authenticationConfig;
    }

    public static StoredUser adminUser() {
        val storedUser = new StoredUser(Utils.hashedId("admin@a.com"), "admin@a.com", "Admin", UserType.HUMAN);
        storedUser.setAuthState(new StoredUserAuthState(AuthMode.PASSWORD, AuthState.ACTIVE, 0, storedUser));
        return storedUser;
    }

    public static StoredUser normalUser() {
        val storedUser = new StoredUser(Utils.hashedId("test@a.com"), "test@a.com", "Test", UserType.HUMAN);
        storedUser.setAuthState(new StoredUserAuthState(AuthMode.PASSWORD, AuthState.ACTIVE, 0, storedUser));
        return storedUser;
    }

    public static StoredUser systemUser() {
        val storedUser = new StoredUser(Utils.hashedId("Test System"),
                                        "testsystem@a.com",
                                        "Test System",
                                        UserType.SYSTEM);
        storedUser.setAuthState(new StoredUserAuthState(AuthMode.TOKEN, AuthState.ACTIVE, 0, storedUser));
        return storedUser;
    }

    public static ClientSession dynamicSession() {
        return new ClientSession("DS1",
                                 Utils.hashedId("test@a.com"),
                                 "S",
                                 "CS1",
                                 TokenType.DYNAMIC,
                                 new Date(System.currentTimeMillis() + 864_00_000),
                                 false,
                                 new Date(),
                                 new Date());
    }

    public static ClientSession staticSession() {
        return new ClientSession("SS1",
                                 Utils.hashedId("test@a.com"),
                                 "S",
                                 "CS1",
                                 TokenType.STATIC,
                                 null,
                                 false,
                                 new Date(),
                                 new Date());
    }

    public static StoredService testService() {
        return testService(false);
    }

    public static StoredService testService(boolean deleted) {
        val service = new StoredService("S", "S", "S", "s.com", "S_S");
        service.setDeleted(deleted);
        return service;
    }

    public static void runInCtx(Runnable r) {
        val ctx = mock(TemplateContext.class);
        doReturn("testpath")
                .when(ctx).lookupTemplatePath(anyString());
        try (MockedStatic<TemplateContext> ctxM = Mockito.mockStatic(TemplateContext.class)) {
            ctxM.when(TemplateContext::getInstance)
                    .thenReturn(ctx);
            r.run();
        }
    }

    public static String randomString(int size) {
        return RandomStringUtils.randomAlphabetic(size);
    }

}
