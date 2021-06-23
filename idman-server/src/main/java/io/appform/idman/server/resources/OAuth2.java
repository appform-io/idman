package io.appform.idman.server.resources;

import com.google.api.client.util.Strings;
import com.google.common.collect.ImmutableMap;
import io.appform.idman.client.IdManClient;
import io.appform.idman.model.TokenInfo;
import io.appform.idman.server.auth.configs.AuthenticationConfig;
import io.appform.idman.server.db.ServiceStore;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.UUID;

/**
 *
 */
@Path("/oauth2")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
public class OAuth2 {
    private static final String ERROR_FIELD = "error";
    private static final String ERROR_DESC_FIELD = "error_description";

    private final IdManClient client;
    private final ServiceStore serviceStore;
    private final AuthenticationConfig authenticationConfig;

    @UtilityClass
    public static class ErrorCodes {
        public static final String INVALID_REQUEST = "invalid_request";
        public static final String UNAUTHORISED_CLIENT = "unauthorized_client";
        public static final String INVALID_CLIENT = "invalid_client";
        public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
        public static final String INVALID_GRANT = "invalid_grant";
    }

    @Inject
    public OAuth2(
            IdManClient client,
            ServiceStore serviceStore,
            AuthenticationConfig authenticationConfig) {
        this.client = client;
        this.serviceStore = serviceStore;
        this.authenticationConfig = authenticationConfig;
    }


    @GET
    @Path("/authorize")
    @Produces(MediaType.APPLICATION_JSON)
    @UnitOfWork
    public Response authorize(
            @QueryParam("response_type") final String type,
            @QueryParam("client_id") final String clientId,
            @QueryParam("state") final String state,
            @QueryParam("redirect_uri") final URI redirectUri) {
        if(null == redirectUri) {
            return badRequest(ErrorCodes.INVALID_REQUEST, "'redirect_uri' is mandatory parameter");

        }
        if(Strings.isNullOrEmpty(type) || Strings.isNullOrEmpty(clientId)) {
            return errorRedirect(redirectUri, ErrorCodes.INVALID_REQUEST,
                                    "'type', 'client_id' and 'redirect_uri' are mandatory parameters");
        }
        if(!type.equals("code")) {
            return errorRedirect(redirectUri, ErrorCodes.UNSUPPORTED_RESPONSE_TYPE,
                                    "Only code grant is supported");
        }
        val service = serviceStore.get(clientId).orElse(null);
        if(null == service) {
            return errorRedirect(redirectUri, ErrorCodes.UNAUTHORISED_CLIENT,
                                    "Unregistered client id. Please use id from IDMan console");
        }
        if(!redirectUri.toString().equalsIgnoreCase(service.getCallbackUrl())) {
            return errorRedirect(redirectUri, ErrorCodes.UNAUTHORISED_CLIENT,
                             "Redirect URI does not match the registered call back uri for service");

        }
        val uri = UriBuilder.fromUri(authenticationConfig.getServer() + "/auth/login/" + clientId)
                .queryParam("redirect", redirectUri)
                .queryParam("clientSessionId",
                            Strings.isNullOrEmpty(state)
                            ? UUID.randomUUID().toString()
                            : state)
                .build();
        return Response.seeOther(uri).build();
    }

    @POST
    @Path("/token")
    @UnitOfWork
    public Response token(
            @FormParam("code") final String code,
            @FormParam("refresh_token") final String refreshToken,
            @FormParam("client_id") final String clientId,
            @FormParam("client_secret") final String clientSecret,
            @FormParam("grant_type") final String grantType) {
        if(Strings.isNullOrEmpty(grantType)) {
            return badRequest(ErrorCodes.INVALID_REQUEST, "'grant_type' is a mandatory parameter");
        }
        if(Strings.isNullOrEmpty(clientId) || Strings.isNullOrEmpty(clientSecret)) {
            return unauthorised(ErrorCodes.INVALID_CLIENT, "'client_id' and 'client_secret' are mandatory");
        }
        val service = serviceStore.get(clientId).orElse(null);
        if (null == service) {
            return unauthorised(ErrorCodes.INVALID_CLIENT, "Unknown client. Please check client id from IDMan console");
        }
        if (!service.getSecret().equals(clientSecret) || service.isDeleted()) {
            return unauthorised(ErrorCodes.INVALID_CLIENT, "Client authentication failure. Please check client id and secret from IDMan console");
        }
        final TokenInfo token;
        if (grantType.equals("authorization_code")) {
            if(Strings.isNullOrEmpty(code)) {
                return badRequest(ErrorCodes.INVALID_REQUEST, "'code' parameter must be provided for grant_type authorization_code");
            }
            token = client.accessToken(clientId, code).orElse(null);
        }
        else if (grantType.equals("refresh_token")) {
            if(Strings.isNullOrEmpty(refreshToken)) {
                return badRequest(ErrorCodes.INVALID_REQUEST, "'refresh_token' parameter must be provided for grant type 'refresh_token'");
            }
            token = client.refreshAccessToken(clientId, refreshToken).orElse(null);
        }
        else {
            token = null;
        }
        if (null == token) {
            return badRequest(ErrorCodes.INVALID_GRANT, "Unknown grant type");
        }
        val user = token.getUser();
        return Response.ok(new TokenInfo(token.getAccessToken(),
                                             token.getRefreshToken(),
                                             token.getExpiry(),
                                             "bearer",
                                             user.getRole(),
                                             user))
                .build();

    }

    private Response badRequest(String errorCode, String errorDescription) {
        return errorResponseRaw(Response.Status.BAD_REQUEST, errorCode, errorDescription);
    }

    private Response unauthorised(String errorCode, String errorDescription) {
        return errorResponseRaw(Response.Status.UNAUTHORIZED, errorCode, errorDescription);
    }

    private Response errorResponseRaw(Response.Status status, String errorCode, String errorDescription) {
        return Response.status(status)
                .entity(ImmutableMap.of(ERROR_FIELD, errorCode, ERROR_DESC_FIELD, errorDescription))
                .build();
    }

    private Response errorRedirect(URI uri, String errorCode, String errorDescription) {
        return Response.seeOther(UriBuilder.fromUri(uri)
                                .queryParam(ERROR_FIELD, errorCode)
                                .queryParam(ERROR_DESC_FIELD, errorDescription)
                                .build())
                .build();

    }
}
