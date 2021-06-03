package io.appform.idman.server.resources;

import io.appform.idman.model.AuthMode;
import io.appform.idman.server.auth.AuthenticationProvider;
import io.appform.idman.server.auth.AuthenticationProviderRegistry;
import io.appform.idman.server.auth.impl.PasswordAuthInfo;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.db.model.SessionType;
import io.appform.idman.server.db.model.StoredUserSession;
import io.appform.idman.server.utils.TestingUtils;
import io.appform.idman.server.views.LoginScreenView;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.Optional;

import static io.appform.idman.server.utils.TestingUtils.runInCtx;
import static io.appform.idman.server.utils.TestingUtils.testService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class AuthTest {
    private AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);
    private AuthenticationProviderRegistry authenticationProviderRegistry = mock(AuthenticationProviderRegistry.class);
    private ServiceStore serviceStore = mock(ServiceStore.class);
    private Auth auth = new Auth(TestingUtils.passwordauthConfig(),
                                 () -> authenticationProviderRegistry,
                                 () -> serviceStore);

    @BeforeEach
    void setup() {
        doReturn(Optional.of(authenticationProvider)).when(authenticationProviderRegistry).provider(AuthMode.PASSWORD);
    }

    @AfterEach
    void destroy() {
        reset(authenticationProvider, authenticationProviderRegistry, serviceStore);
    }

    @Test
    void testScreenRender() {
        runInCtx(() -> {
            val r = auth.loginScreen("S", "a.com", "CS1", "err");
            assertEquals(HttpStatus.SC_OK, r.getStatus());
            assertEquals(LoginScreenView.class, r.getEntity().getClass());
        });
    }

    @Test
    void testPasswordLoginNullService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = auth.passwordLogin(URI.create("/"), "a@a.com", "xx", "/", null, "CS1");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
    }

    @Test
    void testPasswordLoginWrongService() {
        doReturn(Optional.empty()).when(serviceStore).get(anyString());
        val r = auth.passwordLogin(URI.create("/"), "a@a.com", "xx", "/", "S", "CS1");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
    }

    @Test
    void testPasswordLoginDeletedService() {
        val s = testService();
        s.setDeleted(true);
        doReturn(Optional.of(s)).when(serviceStore).get(anyString());
        val r = auth.passwordLogin(URI.create("/"), "a@a.com", "xx", "/", "S", "CS1");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
    }

    @Test
    void testPasswordLoginLoginSuccess() {
        val s = testService();
        val userSession = new StoredUserSession("S1", "U1", "S", "CS1", SessionType.DYNAMIC, null);
        doReturn(Optional.of(s)).when(serviceStore).get(anyString());
        doReturn(Optional.of(userSession))
                .when(authenticationProvider).login(eq(new PasswordAuthInfo("a@a.com", "xx", "S", "CS1")), anyString());
        val r = auth.passwordLogin(URI.create("/"), "a@a.com", "xx", "/", "S", "CS1");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals("s.com", r.getLocation().getPath());
    }

    @Test
    void testPasswordLoginLoginFailure() {
        val s = testService();
        val userSession = new StoredUserSession("S1", "U1", "S", "CS1", SessionType.DYNAMIC, null);
        doReturn(Optional.of(s)).when(serviceStore).get(anyString());
        doReturn(Optional.empty())
                .when(authenticationProvider).login(any(), anyString());
        val r = auth.passwordLogin(URI.create("/"), "a@a.com", "xx", "/", "S", "CS1");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals("/", r.getLocation().getPath());
    }
}
