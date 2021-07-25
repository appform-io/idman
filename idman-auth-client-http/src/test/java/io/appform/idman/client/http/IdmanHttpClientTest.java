package io.appform.idman.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.appform.idman.model.*;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 *
 */
class IdmanHttpClientTest {
    private static final User NORMAL_USER = new User("TU1", "TU", UserType.HUMAN, AuthMode.PASSWORD);

    private static final IdmanUser TEST_USER = new IdmanUser("SS1", "S1", NORMAL_USER, "S_USER");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new ParameterNamesModule());
    }

    private final WireMockServer server = new WireMockServer();

    @BeforeEach
    void setup() {
        server.start();
    }

    @AfterEach
    void destroy() {
        server.stop();
        server.resetAll();
    }

    @Test
    @SneakyThrows
    void testAccessTokenSuccessCall() {
        val tokenInfo = new TokenInfo("T", "T", 60, "bearer", TEST_USER.getRole(), TEST_USER);
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withBody(MAPPER.writeValueAsString(tokenInfo))));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        val r = client.accessToken("S", "T");
        assertTrue(r.isPresent());
        assertEquals(tokenInfo, r.get());
    }

    @Test
    @SneakyThrows
    void testAccessTokenFailedCall() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        assertNull(client.accessToken("S", "T").orElse(null));
    }

    @Test
    @SneakyThrows
    void testValidateSuccessCall() {
        val tokenInfo = new TokenInfo("T", "T", 60, "bearer", TEST_USER.getRole(), TEST_USER);
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withBody(MAPPER.writeValueAsString(tokenInfo))));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        val r = client.validateToken("S", "T");
        assertTrue(r.isPresent());
        assertEquals(tokenInfo, r.get());
    }

    @Test
    @SneakyThrows
    void testValidateTimeoutFailCall() {
        val tokenInfo = new TokenInfo("T", "T", 60, "bearer", TEST_USER.getRole(), TEST_USER);
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withBody(MAPPER.writeValueAsString(tokenInfo))
                                                    .withFixedDelay(2000)));

        val clientConfig = clientConfig();
        clientConfig.setRequestTimeoutMs(100);
        val client = new IdmanHttpClient(clientConfig, MAPPER);
        assertNull(client.validateToken("S", "T").orElse(null));
    }

    @Test
    @SneakyThrows
    void testValidateFailure() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        val r = client.validateToken("S", "T").orElse(null);
        assertNull(r);
    }

    @Test
    @SneakyThrows
    void testValidateException() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/token"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withBody(MAPPER.writeValueAsString(TEST_USER))));

        val mapper = spy(MAPPER);
        doThrow(new IllegalArgumentException("fail"))
                .when(mapper)
                .readValue(anyString(), ArgumentMatchers.eq(IdmanUser.class));
        val client = new IdmanHttpClient(clientConfig(), mapper);
        val r = client.validateToken("S", "T").orElse(null);
        assertNull(r);
    }

    @Test
    @SneakyThrows
    void testDeleteSuccessCall() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/revoke"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)));
        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        assertTrue(client.deleteToken("S", "T"));
    }

    @Test
    @SneakyThrows
    void testDeleteFailureCall() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/revoke"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_SERVICE_UNAVAILABLE)));
        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        assertFalse(client.deleteToken("S", "T"));
    }

    @Test
    @SneakyThrows
    void testDeleteTimeOutExceptionCall() {
        server.stubFor(post(urlEqualTo("/apis/oauth2/revoke"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withFixedDelay(2000)));
        val clientConfig = clientConfig();
        clientConfig.setRequestTimeoutMs(100);
        val client = new IdmanHttpClient(clientConfig, MAPPER);
        assertFalse(client.deleteToken("S", "T"));
    }

    private IdManHttpClientConfig clientConfig() {
        val clientConfig = new IdManHttpClientConfig();
        clientConfig.setAuthEndpoint(server.baseUrl());
        clientConfig.setResourcePrefix("/apis");
        clientConfig.setServiceId("S");
        clientConfig.setAllowedPaths(Collections.singleton("unchecked"));
        return clientConfig;
    }
}