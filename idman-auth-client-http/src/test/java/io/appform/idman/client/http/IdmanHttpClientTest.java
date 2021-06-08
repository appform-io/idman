package io.appform.idman.client.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
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
    void testSuccessCall() {
        server.stubFor(post(urlEqualTo("/apis/auth/check/v1/S"))
                       .willReturn(aResponse()
                                  .withStatus(HttpStatus.SC_OK)
                                  .withBody(MAPPER.writeValueAsString(TEST_USER))));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        val r = client.validate("T", "S");
        assertTrue(r.isPresent());
        assertEquals(TEST_USER, r.get());
    }

    @Test
    @SneakyThrows
    void testFailure() {
        server.stubFor(post(urlEqualTo("/apis/auth/check/v1/S"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));

        val client = new IdmanHttpClient(clientConfig(), MAPPER);
        val r = client.validate("T", "S").orElse(null);
        assertNull(r);
    }

    @Test
    @SneakyThrows
    void testException() {
        server.stubFor(post(urlEqualTo("/apis/auth/check/v1/S"))
                               .willReturn(aResponse()
                                                   .withStatus(HttpStatus.SC_OK)
                                                   .withBody(MAPPER.writeValueAsString(TEST_USER))));

        val mapper = spy(MAPPER);
        doThrow(new IllegalArgumentException("fail"))
                .when(mapper)
                .readValue(anyString(), ArgumentMatchers.eq(IdmanUser.class));
        val client = new IdmanHttpClient(clientConfig(), mapper);
        val r = client.validate("T", "S").orElse(null);
        assertNull(r);
    }

    public IdManHttpClientConfig clientConfig() {
        val clientConfig = new IdManHttpClientConfig();
        clientConfig.setAuthEndpoint(server.baseUrl());
        clientConfig.setResourcePrefix("/apis");
        clientConfig.setServiceId("S");
        clientConfig.setAllowedPaths(Collections.singleton("unchecked"));
        return clientConfig;
    }
}