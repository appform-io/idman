package io.appform.idman.server.resources;

import io.appform.idman.authcomponents.security.ServiceUserPrincipal;
import io.appform.idman.model.TokenType;
import io.appform.idman.server.engine.Engine;
import io.appform.idman.server.engine.ViewEngineResponseTranslator;
import io.appform.idman.server.engine.results.GeneralOpSuccess;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.net.URI;

import static io.appform.idman.server.utils.ServerTestingUtils.runInCtx;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 */
class HomeTest {

    private Engine engine = mock(Engine.class);
    private ViewEngineResponseTranslator translator = new ViewEngineResponseTranslator();
    private Home home = new Home(engine, translator);

    @AfterEach
    void destroy() {
        reset(engine);
    }

    @Test
    void home() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .renderHome(any(), anyString());
        val r = home.home(principal(), "");
        assertResponse(r);
    }

    @Test
    void createService() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .createService(anyString(), anyString(), anyString());
        val r = home.createService("S", "S", "s.com");
        assertResponse(r);
    }

    @Test
    void serviceDetails() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .renderServiceDetails(any(), anyString());
        val r = home.serviceDetails(principal(), "S");
        assertResponse(r);
    }

    @Test
    void updateServiceDescription() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .updateServiceDescription(anyString(), anyString());
        val r = home.updateServiceDescription( "S", "SS");
        assertResponse(r);
    }

    @Test
    void updateServiceCallbackUrl() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .updateServiceCallbackUrl(anyString(), anyString());
        val r = home.updateServiceCallbackUrl( "S", "SS");
        assertResponse(r);
    }

    @Test
    void updateServiceSecret() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .regenerateServiceSecret(anyString());
        val r = home.updateServiceSecret( "S");
        assertResponse(r);
    }

    @Test
    void deleteService() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .deleteService(anyString());
        val r = home.deleteService( "S");
        assertResponse(r);
    }

    @Test
    void createRole() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .createRole(anyString(), anyString(), anyString());
        val r = home.createRole( "S", "R", "RR");
        assertResponse(r);
    }

    @Test
    void updateRole() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .updateRole(anyString(), anyString(), anyString());
        val r = home.updateRole( "S", "R", "rr");
        assertResponse(r);
    }

    @Test
    void deleteRole() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .deleteRole(anyString(), any());
        val r = home.deleteRole("S", "R");
        assertResponse(r);
    }

    @Test
    void createHumanUser() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .createHumanUser(anyString(), anyString(), anyString());
        val r = home.createHumanUser("a@a.com", "A", "AA");
        assertResponse(r);
    }

    @Test
    void createSystemUser() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .createSystemUser(anyString(), anyString());
        val r = home.createSystemUser("a@a.com", "A");
        assertResponse(r);
    }

    @Test
    void userDetails() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .userDetails(any(), anyString());
        val r = home.userDetails(principal(), "A");
        assertResponse(r);
    }

    @Test
    void updateUser() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .updateUser(any(), anyString(), anyString());
        val r = home.updateUser(principal(), "a@a.com", "A");
        assertResponse(r);
    }

    @Test
    void deleteUser() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .deleteUser(anyString());
        val r = home.deleteUser("A");
        assertResponse(r);
    }

    @Test
    void renderPasswordChangePage() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .renderPasswordChangePage(any(), anyString());
        val r = home.renderPasswordChangePage(principal(), "A");
        assertResponse(r);
    }

    @Test
    void changePassword() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .changePassword(any(), anyString(), anyString(), anyString(), anyString());
        val r = home.changePassword(principal(), "A", "p", "q", "q");
        assertResponse(r);
    }

    @Test
    void changePasswordForced() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .changePasswordForced(any(), anyString(), anyString(), anyString());
        val r = home.changePasswordForced(principal(), "A", "q", "q");
        assertResponse(r);
    }

    @Test
    void mapUserToRole() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .mapUserToRole(any(), any(), anyString(), anyString(), anyString());
        val r = home.mapUserToRole(principal(), URI.create("/"), "A", "p", "q");
        assertResponse(r);
    }

    @Test
    void unmapUserFromRole() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .unmapUserFromRole(any(), anyString(), anyString(), anyString());
        val r = home.unmapUserFromRole(URI.create("/"), "A", "p", "q");
        assertResponse(r);
    }

    @Test
    void createStaticSession() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .createStaticSession(anyString(), anyString());
        val r = home.createStaticSession("S", "U1");
        assertResponse(r);
    }

    @Test
    void viewToken() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .viewToken(anyString(), anyString(), anyString());
        val r = home.viewToken("S", "U1", "SS1");
        assertResponse(r);
    }

    @Test
    void deleteToken() {
        doReturn(new GeneralOpSuccess())
                .when(engine)
                .deleteToken(any(), anyString(), anyString(), anyString(), any());
        val r = home.deleteToken(principal(), "A", "p", "q", TokenType.DYNAMIC);
        assertResponse(r);
    }

    @Test
    void newService() {
        runInCtx(() -> {
            val r = home.newService();
            assertEquals(HttpStatus.SC_OK, r.getStatus());
            assertEquals(TemplateView.class, r.getEntity().getClass());
        });
    }

    @Test
    void newUser() {
        runInCtx(() -> {
            val r = home.newUser();
            assertEquals(HttpStatus.SC_OK, r.getStatus());
            assertEquals(TemplateView.class, r.getEntity().getClass());
        });
    }


    private static void assertResponse(javax.ws.rs.core.Response r) {
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    private static ServiceUserPrincipal principal() {
        return new ServiceUserPrincipal(null);
    }

}