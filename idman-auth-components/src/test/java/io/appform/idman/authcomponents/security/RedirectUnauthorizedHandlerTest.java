package io.appform.idman.authcomponents.security;

import io.appform.idman.client.IdmanClientConfig;
import lombok.val;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class RedirectUnauthorizedHandlerTest {

    @Test
    void testResponse() {
        val c = new IdmanClientConfig();
        c.setAuthEndpoint("https://idman.test");
        val r = new RedirectUnauthorizedHandler(c).buildResponse("blah", "FORM");
        assertEquals(HttpStatus.SEE_OTHER_303, r.getStatus());
        assertEquals(URI.create(c.getAuthEndpoint()), r.getLocation());
    }

}