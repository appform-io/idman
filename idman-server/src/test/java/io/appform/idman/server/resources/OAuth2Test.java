package io.appform.idman.server.resources;

import com.fasterxml.jackson.databind.JsonNode;
import io.appform.idman.client.ClientTestingUtils;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.utils.ServerTestingUtils;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class OAuth2Test {
    private static final ServiceStore serviceStore = mock(ServiceStore.class);
    private static final IdManClient client = mock(IdManClient.class);
    private static final ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new OAuth2(client, serviceStore, ServerTestingUtils.passwordauthConfig()))
            .build();
    private static final IdmanUser TEST_USER = new IdmanUser("S1",
                                                             "S",
                                                             new User("U1", "U", UserType.HUMAN, AuthMode.PASSWORD),
                                                             "S_ADMIN");

    @BeforeEach
    void setup() {
        doReturn(Optional.of(ServerTestingUtils.testService()))
                .when(serviceStore)
                .get("S");
        doReturn(Optional.of(ClientTestingUtils.tokenInfo("T", TEST_USER)))
                .when(client)
                .accessToken("S", "testCode");
        doReturn(Optional.of(ClientTestingUtils.tokenInfo("T", TEST_USER)))
                .when(client)
                .validateToken("S", "T");
        doReturn(true)
                .when(client)
                .deleteToken("S", "T");
    }

    @AfterEach
    void teardown() {
        reset(serviceStore);
        reset(client);
    }

    @Test
    void testAuthorizeSuccess() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "S")
                .queryParam("redirect_uri", "s.com")
                .request()
                .buildGet()
                .invoke();

        assertEquals(HttpStatus.SC_SEE_OTHER, response.getStatus());
        assertEquals("/auth/login/S", response.getLocation().getPath());
    }

    @Test
    void testAuthorizeSuccessWitState() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "S")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "s.com")
                .request()
                .buildGet()
                .invoke();

        assertEquals(HttpStatus.SC_SEE_OTHER, response.getStatus());
        val location = response.getLocation();
        assertEquals("/auth/login/S", location.getPath());
        assertEquals("test", URLEncodedUtils.parse(location, StandardCharsets.UTF_8).get(1).getValue());
    }

    @Test
    @SneakyThrows
    void testAuthorizeBadRequestEmptyType() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("client_id", "S")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "http://test.com")
                .request()
                .get();

        matchAuthorizeError(response);
    }

    @Test
    @SneakyThrows
    void testAuthorizeBadRequestEmptyClientId() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "http://test.com")
                .request()
                .get();

        matchAuthorizeError(response);
    }

    @Test
    @SneakyThrows
    void testAuthorizeBadRequestEmptyRedirectUrl() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "S")
                .queryParam("state", "test")
                .request()
                .get();

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
    }

    @Test
    void testAuthorizeInvaliCode() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "token")
                .queryParam("client_id", "S")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "http://test.com")
                .request()
                .buildGet()
                .invoke();

        matchAuthorizeError(response,
                            "unsupported_response_type",
                            "Only code grant is supported");
    }

    @Test
    void testAuthorizeInvalidClient() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "S1")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "http://test.com")
                .request()
                .buildGet()
                .invoke();

        matchAuthorizeError(response,
                            "unauthorized_client",
                            "Unregistered client id. Please use id from IDMan console");
    }

    @Test
    void testAuthorizeInvalidCallbackUrl() {

        val response = EXT.target("/oauth2/authorize")
                .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                .queryParam("response_type", "code")
                .queryParam("client_id", "S")
                .queryParam("state", "test")
                .queryParam("redirect_uri", "http://test.com")
                .request()
                .buildGet()
                .invoke();

        matchAuthorizeError(response,
                            OAuth2.ErrorCodes.UNAUTHORISED_CLIENT,
                            "Redirect URI does not match the registered call back uri for service");
    }

    @Test
    void testCreateTokenSuccess() {

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_OK, response.getStatus());

    }

    @Test
    void testRefreshTokenSuccess() {

        val form = new Form()
                .param("grant_type", "refresh_token")
                .param("refresh_token", "T")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_OK, response.getStatus());

    }

    @Test
    void testTokenFailNoGrant() {

        val form = new Form()
                .param("code", "testCode")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_BAD_REQUEST,
                        OAuth2.ErrorCodes.INVALID_REQUEST,
                        "'grant_type' is a mandatory parameter");

    }

    @Test
    void testTokenFailNoClientId() {
        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_UNAUTHORIZED,
                        OAuth2.ErrorCodes.INVALID_CLIENT,
                        "'client_id' and 'client_secret' are mandatory");

    }

    @Test
    void testTokenFailNoClientSecret() {

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_id", "S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_UNAUTHORIZED,
                        OAuth2.ErrorCodes.INVALID_CLIENT,
                        "'client_id' and 'client_secret' are mandatory");

    }

    @Test
    void testTokenFailWrongClientId() {

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_id", "S1")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_UNAUTHORIZED,
                        OAuth2.ErrorCodes.INVALID_CLIENT,
                        "Unknown client. Please check client id from IDMan console");

    }

    @Test
    void testTokenFailWrongClientSecret() {

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_id", "S")
                .param("client_secret", "WRONG");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_UNAUTHORIZED,
                        OAuth2.ErrorCodes.INVALID_CLIENT,
                        "Client authentication failure. Please check client id and secret from IDMan console");
    }

    @Test
    void testTokenFailDeletedClient() {
        doReturn(Optional.of(ServerTestingUtils.testService(true)))
                .when(serviceStore)
                .get("S");

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("code", "testCode")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_UNAUTHORIZED,
                        OAuth2.ErrorCodes.INVALID_CLIENT,
                        "Client authentication failure. Please check client id and secret from IDMan console");
    }

    @Test
    void testTokenFailNoCode() {

        val form = new Form()
                .param("grant_type", "authorization_code")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_BAD_REQUEST,
                        OAuth2.ErrorCodes.INVALID_REQUEST,
                        "'code' parameter must be provided for grant_type authorization_code");
    }

    @Test
    void testTokenFailNoRefreshToken() {

        val form = new Form()
                .param("grant_type", "refresh_token")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_BAD_REQUEST,
                        OAuth2.ErrorCodes.INVALID_REQUEST,
                        "'refresh_token' parameter must be provided for grant type 'refresh_token'");
    }

    @Test
    void testTokenFailWrongGrant() {

        val form = new Form()
                .param("grant_type", "blah")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/token")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        matchTokenError(response,
                        HttpStatus.SC_BAD_REQUEST,
                        OAuth2.ErrorCodes.INVALID_GRANT,
                        "Unknown grant type");
    }

    @Test
    void testTokenRevokeSuccess() {
        val form = new Form()
                .param("token", "T")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/revoke")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }

    @Test
    void testTokenRevokeInvalidClient() {
        val form = new Form()
                .param("token", "T")
                .param("client_id", "S1")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/revoke")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void testTokenRevokeInvalidClientSecret() {
        val form = new Form()
                .param("token", "T")
                .param("client_id", "S")
                .param("client_secret", "S_S1");
        val response = EXT.target("/oauth2/revoke")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    void testTokenRevokeFail() {
        val form = new Form()
                .param("token", "T1")
                .param("client_id", "S")
                .param("client_secret", "S_S");
        val response = EXT.target("/oauth2/revoke")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.form(form));
        assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }

    @SneakyThrows
    private void matchAuthorizeError(
            Response response,
            String errorCode,
            String errorDescription) {
        assertEquals(HttpStatus.SC_SEE_OTHER, response.getStatus());

        val location = response.getLocation();
        assertEquals("test.com", location.getHost());
        val values = URLEncodedUtils.parse(location, StandardCharsets.UTF_8)
                .stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        assertEquals(errorCode, values.get("error"));
        assertEquals(errorDescription, values.get("error_description"));
    }


    private void matchAuthorizeError(Response response) {
        matchAuthorizeError(
                response,
                OAuth2.ErrorCodes.INVALID_REQUEST,
                "'type', 'client_id' and 'redirect_uri' are mandatory parameters");
    }

    private void matchTokenError(
            Response response,
            int expectedStatus,
            String errorCode,
            String errorDescription) {
        assertEquals(expectedStatus, response.getStatus());


        val values = response.readEntity(JsonNode.class);
        assertEquals(errorCode, values.get("error").asText());
        assertEquals(errorDescription, values.get("error_description").asText());
    }
}