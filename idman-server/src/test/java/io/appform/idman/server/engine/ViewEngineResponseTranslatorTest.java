package io.appform.idman.server.engine;

import io.appform.idman.server.engine.results.*;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.net.URI;

import static io.appform.idman.server.utils.TestingUtils.runInCtx;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class ViewEngineResponseTranslatorTest {

    private final ViewEngineResponseTranslator translator = new ViewEngineResponseTranslator();

    @Test
    void testVisitInvalidUser() {
        val r = translator.translate(new InvalidUser());
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/auth/login"), r.getLocation());
    }

    @Test
    void testVisitCredentialsExpired() {
        val r = translator.translate(new CredentialsExpired("U1"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/users/U1/update/password"), r.getLocation());
    }

    @Test
    void testVisitRedirectToParam() {
        val r = translator.translate(new RedirectToParam("/test"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/test"), r.getLocation());
    }

    @Test
    void testVisitViewOpSuccess() {
        runInCtx(() -> {
            final TemplateView view = new TemplateView("test");
            val r = translator.translate(new ViewOpSuccess(view));
            assertEquals(HttpStatus.SC_OK, r.getStatus());
            assertEquals(view, r.getEntity());
        });
    }

    @Test
    void testVisitInvalidService() {
        val r = translator.translate(new InvalidService());
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    @Test
    void testVisitServiceOpSuccess() {
        val r = translator.translate(new ServiceOpSuccess("S"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/services/S"), r.getLocation());
    }

    @Test
    void testVisitGeneralOpSuccess() {
        val r = translator.translate(new GeneralOpSuccess());
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    @Test
    void testVisitGeneralOpFailure() {
        val r = translator.translate(new GeneralOpFailure());
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    @Test
    void testVisitUserOpSuccess() {
        val r = translator.translate(new UserOpSuccess("U1"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/users/U1"), r.getLocation());
    }

    @Test
    void testVisitUserOpFailure() {
        val r = translator.translate(new UserOpFailure("U1"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/users/U1"), r.getLocation());
    }

    @Test
    void testVisitRoleOpFailure() {
        val r = translator.translate(new RoleOpFailure("S1", "U1"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/services/S1"), r.getLocation());
    }

    @Test
    void testVisitRoleOpSuccess() {
        val r = translator.translate(new RoleOpSuccess("S1", "U1"));
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/services/S1"), r.getLocation());
    }
}