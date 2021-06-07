package io.appform.idman.authcomponents.resource;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 */
@PermitAll
@Produces(MediaType.TEXT_PLAIN)
@Path("/")
public class TestResource {

    @Path("/all")
    @GET
    public Response all() {
        return Response.ok("Hello all").build();
    }

    @Path("/admin")
    @GET
    @RolesAllowed("S_ADMIN")
    public Response admin() {
        return Response.ok("Hello admin").build();
    }

    @Path("/unchecked")
    @GET
    public Response unchecked() {
        return Response.ok("Hello stranger").build();
    }

}
