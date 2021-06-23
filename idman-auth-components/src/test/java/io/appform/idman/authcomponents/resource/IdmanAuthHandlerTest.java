package io.appform.idman.authcomponents.resource;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.collect.ImmutableSet;
import io.appform.idman.authcomponents.IdmanAuthDynamicFeature;
import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.client.IdManClient;
import io.appform.idman.client.IdmanClientConfig;
import io.appform.idman.model.*;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static io.appform.idman.client.ClientTestingUtils.clientConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class IdmanAuthHandlerTest {
    private static final User ADMIN_USER = new User("TA1", "TA", UserType.HUMAN, AuthMode.PASSWORD);
    private static final User NORMAL_USER = new User("TU1", "TU", UserType.HUMAN, AuthMode.PASSWORD);
    private static final IdmanUser TEST_ADMIN = new IdmanUser("SS1", "S1", ADMIN_USER, "S_ADMIN");

    private static final IdmanUser TEST_USER = new IdmanUser("SS1", "S1", NORMAL_USER, "S_USER");

    private static final Environment environment = mock(Environment.class);
    private final IdManClient idmanClient = mock(IdManClient.class);
    private final CloseableHttpClient client = HttpClients.custom()
            .disableRedirectHandling()
            .build();
    private final IdmanClientConfig config = clientConfig();

    {
        config.setAllowedPaths(ImmutableSet.of("/", "/callback"));
    }

    static {
        doReturn(SharedMetricRegistries.getOrCreate("test")).when(environment).metrics();
//        doReturn(mock(JerseyEnvironment.class)).when(environment).jersey();
    }

    private final ResourceExtension EXT = ResourceExtension.builder()
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .addProvider(new IdmanAuthDynamicFeature(environment, config, idmanClient))
            .addProvider(RolesAllowedDynamicFeature.class)
            .addProvider(new AuthValueFactoryProvider.Binder<>(ServiceUserPrincipal.class))
            .addResource(new IdmanAuthHandler(idmanClient, config))
            .build();

    @BeforeEach
    void setup() {
        doReturn(Optional.of(new TokenInfo("ADMIN_TOKEN", "ADMIN_TOKEN", 60, "bearer", TEST_ADMIN.getRole(), TEST_ADMIN)))
                         .when(idmanClient)
                         .accessToken(anyString(), eq("ADMIN_TOKEN"));
        doReturn(Optional.of(new TokenInfo("USER_TOKEN", "USER_TOKEN", 60, "bearer", TEST_USER.getRole(), TEST_USER)))
                .when(idmanClient)
                .accessToken(anyString(), eq("USER_TOKEN"));
        doReturn(Optional.empty())
                .when(idmanClient)
                .accessToken(anyString(), eq("WRONG_TOKEN"));
        doReturn(Optional.of(new TokenInfo("ADMIN_TOKEN", "ADMIN_TOKEN", 60, "bearer", TEST_ADMIN.getRole(), TEST_ADMIN)))
                .when(idmanClient)
                .validateToken(anyString(), eq("ADMIN_TOKEN"));
        doReturn(Optional.of(new TokenInfo("USER_TOKEN", "USER_TOKEN", 60, "bearer", TEST_USER.getRole(), TEST_USER)))
                .when(idmanClient)
                .validateToken(anyString(), eq("USER_TOKEN"));
        doReturn(Optional.empty())
                .when(idmanClient)
                .validateToken(anyString(), eq("WRONG_TOKEN"));
    }

    @AfterEach
    void destroy() {
        reset(idmanClient);
    }

    @Test
    @SneakyThrows
    void testStartAuth() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);

        val get = new HttpGet(EXT.target("/idman/auth").getUri());
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val u = URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue());
            assertEquals("/apis/oauth2/authorize", u.getPath());
            assertEquals("localhost", u.getHost());
            assertEquals(8080, u.getPort());
            assertEquals("http", u.getScheme());
            val q = URLEncodedUtils.parse(u.getQuery(), StandardCharsets.UTF_8)
                    .stream()
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
            assertEquals("http://localhost:3000/apis/idman/auth/callback", q.get("redirect_uri"));
            assertEquals("testservice", q.get("client_id"));
            assertEquals("code", q.get("response_type"));
            val clientSessionId = q.get("state");
            assertTrue(clientSessionId
                               .matches("[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}"));
            assertEquals(referer, cs.getCookies().stream().filter(c -> c.getName().equals("idman-local-redirect")).map(
                    Cookie::getValue).collect(Collectors.toSet()).stream().findAny().orElse(null));
            assertEquals(clientSessionId, cs.getCookies()
                    .stream()
                    .filter(c -> c.getName().equals("idman-auth-state"))
                    .map(
                            Cookie::getValue)
                    .collect(Collectors.toSet())
                    .stream()
                    .findAny()
                    .orElse(null));
        }
    }

    @Test
    @SneakyThrows
    void testStartAuthWithError() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);

        val uri = new URIBuilder(EXT.target("/idman/auth").getUri())
                .setParameter("error", "Test Error")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val u = URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue());
            assertEquals("/apis/oauth2/authorize", u.getPath());
            assertEquals("localhost", u.getHost());
            assertEquals(8080, u.getPort());
            assertEquals("http", u.getScheme());
            val q = URLEncodedUtils.parse(u.getQuery(), StandardCharsets.UTF_8)
                    .stream()
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
            assertEquals("http://localhost:3000/apis/idman/auth/callback", q.get("redirect_uri"));
            assertEquals("Test Error", q.get("error"));
            assertEquals("testservice", q.get("client_id"));
            assertEquals("code", q.get("response_type"));
            val clientSessionId = q.get("state");
            assertTrue(clientSessionId
                               .matches("[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}"));
            assertEquals(referer, cs.getCookies().stream().filter(c -> c.getName().equals("idman-local-redirect")).map(
                    Cookie::getValue).collect(Collectors.toSet()).stream().findAny().orElse(null));
            assertEquals(clientSessionId, cs.getCookies()
                    .stream()
                    .filter(c -> c.getName().equals("idman-auth-state"))
                    .map(
                            Cookie::getValue)
                    .collect(Collectors.toSet())
                    .stream()
                    .findAny()
                    .orElse(null));
        }
    }

    @Test
    @SneakyThrows
    void testStartAuthWithRedirect() {
        val ic = new BasicClientCookie("idman-local-redirect", "/protected");
        ic.setDomain("localhost");
        ic.setPath("/");
        val cs = new BasicCookieStore();
        cs.addCookie(ic);
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val get = new HttpGet(EXT.target("/idman/auth").getUri());
        get.setHeader(HttpHeaders.REFERER, "https://mydomain.com");
        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val u = URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue());
            assertEquals("/apis/oauth2/authorize", u.getPath());
            assertEquals("localhost", u.getHost());
            assertEquals(8080, u.getPort());
            assertEquals("http", u.getScheme());
            val q = URLEncodedUtils.parse(u.getQuery(), StandardCharsets.UTF_8)
                    .stream()
                    .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
            assertEquals("http://localhost:3000/apis/idman/auth/callback", q.get("redirect_uri"));
            val clientSessionId = q.get("state");
            assertTrue(clientSessionId
                               .matches("[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}"));
            assertEquals("testservice", q.get("client_id"));
            assertEquals("code", q.get("response_type"));
            assertEquals("/protected", cs.getCookies()
                    .stream()
                    .filter(c -> c.getName().equals("idman-local-redirect"))
                    .map(
                            Cookie::getValue)
                    .collect(Collectors.toSet())
                    .stream()
                    .findAny()
                    .orElse(null));
            assertEquals(clientSessionId, cs.getCookies()
                    .stream()
                    .filter(c -> c.getName().equals("idman-auth-state"))
                    .map(
                            Cookie::getValue)
                    .collect(Collectors.toSet())
                    .stream()
                    .findAny()
                    .orElse(null));
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallback() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val csId = UUID.randomUUID().toString();
        addCookie(cs, "idman-auth-state", csId);
        addCookie(cs, "idman-local-redirect", "/protected");
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "USER_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val u = URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue());
            assertEquals("/protected", u.getPath());
            assertEquals("localhost", u.getHost());
            assertEquals(uri.getPort(), u.getPort());
            assertEquals("http", u.getScheme());

            assertEquals("USER_TOKEN", cs.getCookies().stream().filter(c -> c.getName()
                    .equals("idman-token-testservice")).map(
                    Cookie::getValue).collect(Collectors.toSet()).stream().findAny().orElse(null));
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallbackEmpty() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val csId = UUID.randomUUID().toString();
        addCookie(cs, "idman-auth-state", csId);
        addCookie(cs, "idman-local-redirect", "");
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "USER_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            val u = URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue());
            assertEquals("/", u.getPath());
            assertEquals("localhost", u.getHost());
            assertEquals(uri.getPort(), u.getPort());
            assertEquals("http", u.getScheme());

            assertEquals("USER_TOKEN", cs.getCookies().stream().filter(c -> c.getName()
                    .equals("idman-token-testservice")).map(
                    Cookie::getValue).collect(Collectors.toSet()).stream().findAny().orElse(null));
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallbackMissingRedirect() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val csId = UUID.randomUUID().toString();
        addCookie(cs, "idman-auth-state", csId);
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "USER_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallbackMissingCSID() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val csId = UUID.randomUUID().toString();
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "USER_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallbackWrongToken() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        val csId = UUID.randomUUID().toString();
        addCookie(cs, "idman-auth-state", csId);
        addCookie(cs, "idman-local-redirect", "");
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "WRONG_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SneakyThrows
    void testHandleCallbackWrongCSID() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);
        addCookie(cs, "idman-auth-state", "abc");
        addCookie(cs, "idman-local-redirect", "");
        val csId = UUID.randomUUID().toString();
        val uri = new URIBuilder(EXT.target("/idman/auth/callback").getUri())
                .addParameter("state", csId)
                .addParameter("code", "USER_TOKEN")
                .build();
        val get = new HttpGet(uri);
        val referer = "https://mydomain.com";
        get.setHeader(HttpHeaders.REFERER, referer);

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
        }
    }

    @Test
    @SneakyThrows
    void testLogout() {
        val cs = new BasicCookieStore();
        val ctx = new BasicHttpContext();
        ctx.setAttribute(HttpClientContext.COOKIE_STORE, cs);

        val get = new HttpPost(EXT.target("/idman/auth/logout").getUri());
        get.addHeader(HttpHeaders.AUTHORIZATION, "Bearer USER_TOKEN");

        try (val r = client.execute(get, ctx)) {
            assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatusLine().getStatusCode());
            assertEquals("/",
                         URI.create(r.getLastHeader(HttpHeaders.LOCATION).getValue()).getPath());
        }
    }

    private void addCookie(BasicCookieStore cs, String name, String csId) {
        val csc = new BasicClientCookie(name, csId);
        csc.setDomain("localhost");
        csc.setPath("/");
        cs.addCookie(csc);
    }
}