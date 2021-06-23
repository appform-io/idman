package io.appform.idman.server.resources;

import io.appform.idman.server.views.ErrorView;
import ru.vyarus.guicey.gsp.views.template.Template;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 *
 */
@Path("/ui/error")
@Template
public class ErrorPage {
    @GET
    public Response showError(@QueryParam("errorCode") final String errorCode,
                              @QueryParam("description") final String errorDescription) {
        return Response.ok(new ErrorView(errorCode, errorDescription)).build();
    }
}
