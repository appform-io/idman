package io.appform.idman.server.auth;

import io.appform.idman.model.TokenType;
import io.appform.idman.server.auth.configs.JwtConfig;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.SessionStore;
import io.appform.idman.server.db.UserInfoStore;
import io.appform.idman.server.db.UserRoleStore;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import io.appform.idman.server.utils.ServerTestingUtils;
import io.appform.idman.server.utils.Utils;
import io.dropwizard.util.Duration;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *
 */
class TokenManagerTest {
    private final UserInfoStore userInfoStore = mock(UserInfoStore.class);
    private final ServiceStore serviceStore = mock(ServiceStore.class);
    private final SessionStore sessionStore = mock(SessionStore.class);
    private final UserRoleStore roleStore = mock(UserRoleStore.class);
    private final JwtConfig jwtConfig = ServerTestingUtils.passwordauthConfig().getJwt();

    private final TokenManager tokenManager = new TokenManager(userInfoStore,
                                                               serviceStore,
                                                               sessionStore,
                                                               roleStore,
                                                               jwtConfig);

    @AfterEach
    void destroy() {
        reset(userInfoStore,
              serviceStore,
              sessionStore,
              roleStore);
    }

    @Test
    void testCreateTokenSuccess() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);
        val expiry = Utils.futureTime(Duration.days(7));
        doReturn(Optional.of(ServerTestingUtils.dynamicSession()))
                .when(sessionStore)
                .create(anyString(),
                        eq(user.getUserId()),
                        eq(testService.getServiceId()),
                        eq("CS1"),
                        eq(TokenType.DYNAMIC),
                        eq(expiry));
        val r = tokenManager.createToken(testService.getServiceId(), user.getUserId(), "CS1", TokenType.DYNAMIC, expiry)
                .orElse(null);
        assertNotNull(r);
    }

    @Test
    void testCreateTokenSuccessStatic() {
        val user = ServerTestingUtils.systemUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);
        doReturn(Optional.of(ServerTestingUtils.dynamicSession()))
                .when(sessionStore)
                .create(anyString(),
                        eq(user.getUserId()),
                        eq(testService.getServiceId()),
                        isNull(),
                        eq(TokenType.STATIC),
                        isNull());
        val r = tokenManager.createToken(testService.getServiceId(), user.getUserId(), null, TokenType.STATIC, null)
                .orElse(null);
        assertNotNull(r);
    }

    @Test
    void testCreateTokenFailExpMismatch() {
        {
            assertNull(tokenManager.createToken("S",
                                                "U1",
                                                "CS1",
                                                TokenType.DYNAMIC,
                                                null)
                               .orElse(null));
        }
        {
            assertNull(tokenManager.createToken("S",
                                                "U1",
                                                "CS1",
                                                TokenType.STATIC,
                                                new Date())
                               .orElse(null));
        }
    }

    @Test
    void testCreateTokenFailNoService() {
        val user = ServerTestingUtils.normalUser();
        doReturn(Optional.of(user))
                .when(userInfoStore)
                .get(user.getUserId());
        doReturn(Optional.empty())
                .when(serviceStore)
                .get(anyString());
        val expiry = Utils.futureTime(Duration.days(7));
        doReturn(Optional.of(ServerTestingUtils.dynamicSession()))
                .when(sessionStore)
                .create(anyString(),
                        eq(user.getUserId()),
                        eq("S"),
                        eq("CS1"),
                        eq(TokenType.DYNAMIC),
                        eq(expiry));

        assertNull(tokenManager.createToken("S",
                                            user.getUserId(),
                                            "CS1",
                                            TokenType.DYNAMIC,
                                            expiry)
                           .orElse(null));
    }

    @Test
    void testCreateTokenFailDelService() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);
        val expiry = Utils.futureTime(Duration.days(7));
        doReturn(Optional.of(ServerTestingUtils.dynamicSession()))
                .when(sessionStore)
                .create(anyString(),
                        eq(user.getUserId()),
                        eq(testService.getServiceId()),
                        eq("CS1"),
                        eq(TokenType.DYNAMIC),
                        eq(expiry));

        testService.setDeleted(true);
        assertNull(tokenManager.createToken(testService.getServiceId(),
                                            user.getUserId(),
                                            "CS1",
                                            TokenType.DYNAMIC,
                                            expiry)
                           .orElse(null));
    }

    @Test
    void testCreateTokenFailDelUser() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);
        val expiry = Utils.futureTime(Duration.days(7));
        doReturn(Optional.of(ServerTestingUtils.dynamicSession()))
                .when(sessionStore)
                .create(anyString(),
                        eq(user.getUserId()),
                        eq(testService.getServiceId()),
                        eq("CS1"),
                        eq(TokenType.DYNAMIC),
                        eq(expiry));
        user.setDeleted(true);
        assertNull(tokenManager.createToken(testService.getServiceId(),
                                            user.getUserId(),
                                            "CS1",
                                            TokenType.DYNAMIC,
                                            expiry)
                           .orElse(null));
    }

    @Test
    void testCreateTokenFailNoUser() {
        doReturn(Optional.empty())
                .when(userInfoStore)
                .get(anyString());
        val testService = ServerTestingUtils.testService();
        doReturn(Optional.of(testService))
                .when(serviceStore)
                .get(testService.getServiceId());
        val expiry = Utils.futureTime(Duration.days(7));

        assertNull(tokenManager.createToken(testService.getServiceId(),
                                            "U1",
                                            "CS1",
                                            TokenType.DYNAMIC,
                                            expiry)
                           .orElse(null));
    }

    @Test
    void testCreateTokenFailWrongType() {
        val user = ServerTestingUtils.systemUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);
        val expiry = Utils.futureTime(Duration.days(7));

        assertNull(tokenManager.createToken(testService.getServiceId(),
                                            user.getUserId(),
                                            "CS1",
                                            TokenType.DYNAMIC,
                                            expiry)
                           .orElse(null));
    }

    @Test
    void testCreateTokenFailWrongTypeNorm() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        setupStores(user, testService);

        assertNull(tokenManager.createToken(testService.getServiceId(), user.getUserId(), "CS1", TokenType.STATIC, null)
                           .orElse(null));
    }

    @Test
    void generateTokenForSessionNoRoleFailure() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        val session = ServerTestingUtils.dynamicSession();
        setupStores(user, testService);
        doReturn(Optional.of(session))
                .when(sessionStore)
                .get(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(true)
                .when(sessionStore)
                .delete(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(Optional.empty())
                .when(roleStore)
                .getUserServiceRole(user.getUserId(), testService.getServiceId());
        val ti = tokenManager.generateTokenForSession(testService.getServiceId(), session.getSessionId(), TokenType.DYNAMIC)
                .orElse(null);
        assertNull(ti);
    }

    @Test
    void testDeleteTokenSuccess() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        val session = ServerTestingUtils.dynamicSession();
        setupStores(user, testService);
        doReturn(Optional.of(session))
                .when(sessionStore)
                .get(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(true)
                .when(sessionStore)
                .delete(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(Optional.of(ServerTestingUtils.normalRole(user.getUserId())))
                .when(roleStore)
                .getUserServiceRole(user.getUserId(), testService.getServiceId());
        val ti = tokenManager.generateTokenForSession(testService.getServiceId(), session.getSessionId(), TokenType.DYNAMIC)
                .orElse(null);
        assertNotNull(ti);
        assertTrue(tokenManager.deleteToken(session.getServiceId(), ti.getToken()));
    }

    @Test
    void testDeleteTokenFailDeletedService() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        val expiry = Utils.futureTime(Duration.days(7));
        val session = ServerTestingUtils.dynamicSession();
        setupStores(user, testService);
        testService.setDeleted(true);
        doReturn(Optional.of(session))
                .when(sessionStore)
                .get(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(true)
                .when(sessionStore)
                .delete(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(Optional.of(ServerTestingUtils.normalRole(user.getUserId())))
                .when(roleStore)
                .getUserServiceRole(user.getUserId(), testService.getServiceId());
        val ti = tokenManager.generateTokenForSession(testService.getServiceId(), session.getSessionId(), TokenType.DYNAMIC)
                .orElse(null);
        assertNotNull(ti);
        assertFalse(tokenManager.deleteToken(session.getServiceId(), ti.getToken()));
    }

    @Test
    void testDeleteTokenFailSessionDeleteFailure() {
        val user = ServerTestingUtils.normalUser();
        val testService = ServerTestingUtils.testService();
        val session = ServerTestingUtils.dynamicSession();
        setupStores(user, testService);

        doReturn(Optional.of(session))
                .when(sessionStore)
                .get(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(false)
                .when(sessionStore)
                .delete(session.getSessionId(), TokenType.DYNAMIC);
        doReturn(Optional.of(ServerTestingUtils.normalRole(user.getUserId())))
                .when(roleStore)
                .getUserServiceRole(user.getUserId(), testService.getServiceId());

        val ti = tokenManager.generateTokenForSession(testService.getServiceId(), session.getSessionId(), TokenType.DYNAMIC)
                .orElse(null);
        assertNotNull(ti);
        assertFalse(tokenManager.deleteToken(session.getServiceId(), ti.getToken()));
    }

    private void setupStores(StoredUser user, StoredService testService) {
        doReturn(Optional.of(user))
                .when(userInfoStore)
                .get(user.getUserId());
        doReturn(Optional.of(testService))
                .when(serviceStore)
                .get(testService.getServiceId());
    }

}