/*
 * Copyright 2021. Santanu Sinha
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package io.appform.idman.server.resources;

import io.appform.idman.model.TokenInfo;
import io.appform.idman.server.db.ServiceStore;
import io.appform.idman.server.localauth.LocalIdmanClient;
import io.dropwizard.hibernate.UnitOfWork;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotEmpty;
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
    private final Provider<LocalIdmanClient> client;

    @Inject
    public Apis(Provider<ServiceStore> serviceStore, Provider<LocalIdmanClient> client) {
        this.serviceStore = serviceStore;
        this.client = client;
    }

    @Path("/check/v1/{serviceId}")
    @POST
    @UnitOfWork
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response validateToken(
            @HeaderParam(HttpHeaders.AUTHORIZATION) @NotEmpty final String authorization,
            @PathParam("serviceId") @NotEmpty final String serviceId,
            @FormParam("token") @NotEmpty final String token) {
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
        val validatedUser = client.get()
                .validateToken(serviceId, token)
                .map(TokenInfo::getUser)
                .orElse(null);
        if(null == validatedUser) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(validatedUser).build();
    }
}
