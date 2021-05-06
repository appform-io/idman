package io.appform.idman.server.auth.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.auth.AuthenticationProviderFactory;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.PasswordStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.impl.DBPasswordStore;
import io.appform.idman.server.db.impl.DBSessionStore;
import io.appform.idman.server.db.impl.DBUserInfoStore;
import io.appform.idman.server.db.model.StoredPassword;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.db.model.StoredUserAuthState;
import io.appform.idman.server.db.model.StoredUserSession;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.util.Duration;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class PasswordAuthenticationProviderTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(StoredUser.class)
            .addEntityClass(StoredUserAuthState.class)
            .addEntityClass(StoredPassword.class)
            .addEntityClass(StoredUserSession.class)
            .build();

    private UserInfoStore userInfoStore;
    private PasswordStore passwordStore;
    private SessionStore sessionStore;
    private AuthenticationConfig authenticationConfig;

    @BeforeEach
    void setup() {
        userInfoStore = new DBUserInfoStore(database.getSessionFactory());
        passwordStore = new DBPasswordStore(database.getSessionFactory());
        sessionStore = new DBSessionStore(database.getSessionFactory());

        val user = database.inTransaction(() -> userInfoStore.create("UI",
                                                                     "u@u.t",
                                                                     "U",
                                                                     UserType.HUMAN,
                                                                     AuthMode.PASSWORD)).orElse(null);
        Objects.requireNonNull(user);
        database.inTransaction(() -> passwordStore.set(user.getUserId(), "TESTPASSWORD"));

        authenticationConfig = new AuthenticationConfig();
        authenticationConfig.setDomain("testd");
        authenticationConfig.setMode(AuthMode.PASSWORD);
        authenticationConfig.setServer("localhost");
        authenticationConfig.setSessionDuration(Duration.days(7));

        val jwtConfig = new JwtConfig();
        jwtConfig.setIssuerId("testissuer");
        jwtConfig.setPrivateKey(
                "bYdNUUyCqx8IuGNqhFYS27WizZrfupAmJS8I4mfj2Cjox9Nc04Oews9tJEiDTrJfopzKdjygi8SgXeopSe/rPYqEKfrAUw/Dn6wMVhE56S7/5DKAvYusA2fQRqxOrOosO1lERnArw15tkAf/z5QUUUXnKZZTiczNEebjs2OG5s94PGxtQzxtYsZ1q2oXoq4lKPTosPpwkRxeh8LQCweDGR80xgoM1+yDAoYIeg==");
        authenticationConfig.setJwt(jwtConfig);
    }

    @Test
    void testPasswordAuth() {
        val af = new AuthenticationProviderFactory(authenticationConfig, new ObjectMapper(), () -> userInfoStore, () -> sessionStore, () -> passwordStore);

        val authProvider = af.create(authenticationConfig.getProvider());
        val user = userInfoStore.get("UI").orElse(null);
        {
            assertNotNull(user);
            val resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNotNull(resp);
        }
        {
            val resp = authProvider.login(new PasswordAuthInfo("u@u.t", "TESTPASSWORD1", "S1", "CS1"), "S1")
                    .orElse(null);
            assertNull(resp);
        }
        {
            try {
                authProvider.redirectionURL("xx");
                fail("Should have thrown unsupported operation exception");
            } catch (UnsupportedOperationException e) {
                assertEquals("This is not yet implemented", e.getMessage());
            }
        }
        {
            try {
                authProvider.login(new GoogleAuthInfo("XX", "xx", "xx"), "S1");
                fail("Expected illegal argument exception");
            } catch (IllegalArgumentException e) {
                assertEquals("Google auth info passed to password authenticator", e.getMessage());
            }

        }
    }
}