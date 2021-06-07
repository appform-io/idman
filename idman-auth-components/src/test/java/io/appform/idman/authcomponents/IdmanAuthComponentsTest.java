package io.appform.idman.authcomponents;

import com.codahale.metrics.SharedMetricRegistries;
import io.appform.idman.authcomponents.resource.TestResource;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@Slf4j
class IdmanAuthComponentsTest {
    private static final User ADMIN_USER = new User("TA1", "TA", UserType.HUMAN, AuthMode.PASSWORD);
    private static final User NORMAL_USER = new User("TU1", "TU", UserType.HUMAN, AuthMode.PASSWORD);
    private static final IdmanUser TEST_ADMIN = new IdmanUser("SS1", "S1", ADMIN_USER, "S_ADMIN");

    private static final IdmanUser TEST_USER = new IdmanUser("SS1", "S1", NORMAL_USER, "S_USER");

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
            .addProvider(new IdmanAuthDynamicFeature(environment, ClientTestingUtils.clientConfig(), idmanClient))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class))
            .addResource(new TestResource())
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
    void testAuthUserSuccess() {
        assertEquals(HttpStatus.SC_OK, makeRequest("/all", "USER_TOKEN"));
    }

    @Test
    void testAuthUserRoleFail() {
        assertEquals(HttpStatus.SC_FORBIDDEN, makeRequest("/admin", "USER_TOKEN"));
    }

    @Test
    void testAuthAdminRoleSuccess() {
        assertEquals(HttpStatus.SC_OK, makeRequest("/admin", "ADMIN_TOKEN"));
    }

    @Test
    void testAuthWrongTokenFail() {
        assertEquals(HttpStatus.SC_SEE_OTHER, makeRequest("/admin", "WRONG_TOKEN"));
    }

    @Test
    void testAuthUserUncheckedPass() {
        assertEquals(HttpStatus.SC_OK, makeRequest("/unchecked", "WRONG_TOKEN"));
    }

    @Test
    @SneakyThrows
    void testAuthUserNoAuth() {
        val get = new HttpGet(EXT.target("/all").getUri());
        try (val r = client.execute(get)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            assertEquals("/apis/idman/auth",
                         URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue()).getPath());
        }
    }


    @Test
    @SneakyThrows
    void testAuthUserUsingWrongCookie() {
        val cookieStore = new BasicCookieStore();
        val c = new BasicClientCookie("idman-token-testservice", "WRONG_TOKEN");
        c.setDomain("localhost");
        c.setPath("/");
        cookieStore.addCookie(c);

        val get = new HttpGet(EXT.target("/all").getUri());
        val ctx = new BasicHttpContext();

        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            assertEquals("/apis/idman/auth",
                         URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue()).getPath());
        }
    }

    @Test
    @SneakyThrows
    void testAuthUserUsingUserCookie() {
        val cookieStore = new BasicCookieStore();
        val c = new BasicClientCookie("idman-token-testservice", "USER_TOKEN");
        c.setDomain("localhost");
        c.setPath("/");
        cookieStore.addCookie(c);

        val get = new HttpGet(EXT.target("/all").getUri());
        val ctx = new BasicHttpContext();

        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_OK, r.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SneakyThrows
    void testAuthUserErrorPropagationTest() {
        final BasicNameValuePair param = new BasicNameValuePair("error", "TestError");
        final URI uri = new URIBuilder(EXT.target("/all").getUri())
                .setParameters(param)
                .build();
        val get = new HttpGet(uri);
        get.addHeader(HttpHeaders.AUTHORIZATION, "Bearer WRONG_TOKEN");
        try (val r = client.execute(get)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val q = URLEncodedUtils.parse(URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue()),
                                          StandardCharsets.UTF_8);
            assertTrue(q.contains(param));
        }
    }


    @SneakyThrows
    private int makeRequest(String path, String token) {
        val get = new HttpGet(EXT.target(path).getUri());
        get.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        try (val r = client.execute(get)) {
            return r.getStatusLine().getStatusCode();
        }
    }
}