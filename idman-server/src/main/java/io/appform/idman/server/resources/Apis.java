package io.appform.idman.server.resources;

import com.google.common.base.Strings;
import io.appform.idman.server.localauth.LocalIdmanAuthClient;
import io.appform.idman.server.db.ServiceStore;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/auth")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
public class Apis {

    private final Provider<ServiceStore> serviceStore;
    private final Provider<LocalIdmanAuthClient> client;

    @Inject
    public Apis(Provider<ServiceStore> serviceStore, Provider<LocalIdmanAuthClient> client) {
        this.serviceStore = serviceStore;
        this.client = client;
    }

    @Path("/check/v1/{serviceId}")
    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateToken(
            @HeaderParam(HttpHeaders.AUTHORIZATION) final String authorization,
            @PathParam("serviceId") final String serviceId,
            @FormParam("token") final String token) {
        if (Strings.isNullOrEmpty(authorization)
                || Strings.isNullOrEmpty(serviceId)
                || Strings.isNullOrEmpty(token)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        val service = serviceStore.get().get(serviceId).orElse(null);
        if (null == service) {
            log.error("Invalid service id provided for token validation: {}", serviceId);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        final String[] parts = authorization.split("Bearer ");
        if(parts.length != 2) {
            log.error("Invalid auth secret sent for: {}", serviceId);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        val providedSecret = parts[1];
        if (!service.getSecret().equals(providedSecret)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        val validatedUser = client.get().validate(token, serviceId).orElse(null);
        if(null == validatedUser) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(validatedUser).build();
    }
}
