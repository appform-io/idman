package io.appform.idman.server.resources;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.UserType;
import io.appform.idman.server.db.*;
import io.appform.idman.server.db.model.StoredRole;
import io.appform.idman.server.utils.TestingUtils;
import io.appform.idman.server.views.NewUserView;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URI;
import java.util.Optional;

import static io.appform.idman.server.utils.TestingUtils.runInCtx;
import static io.appform.idman.server.utils.TestingUtils.testService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 *
 */
class FirstTimeWizardTest {
    private UserInfoStore userInfoStore = mock(UserInfoStore.class);
    private PasswordStore passwordStore = mock(PasswordStore.class);
    private UserRoleStore userRoleStore = mock(UserRoleStore.class);
    private ServiceStore serviceStore = mock(ServiceStore.class);
    private RoleStore roleStore = mock(RoleStore.class);
    private FirstTimeWizard firstTimeWizard = new FirstTimeWizard(() -> userInfoStore,
                                                                  () -> passwordStore,
                                                                  () -> userRoleStore,
                                                                  () -> serviceStore,
                                                                  () -> roleStore);

    @AfterEach
    void destroy() {
        reset(userInfoStore, passwordStore, userRoleStore, serviceStore, roleStore);
    }

    @Test
    void testSetupScreenIdmanExistsFail() {
        val testService = testService();
        doReturn(Optional.of(testService)).when(serviceStore).get("IDMAN");
        val r = firstTimeWizard.setupScreen();
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    @Test
    void testSetupScreenSuccess() {
        doReturn(Optional.empty()).when(serviceStore).get("IDMAN");
        runInCtx(() -> {
            val r = firstTimeWizard.setupScreen();
            assertEquals(HttpStatus.SC_OK, r.getStatus());
            assertEquals(NewUserView.class, r.getEntity().getClass());
        });
    }

    @Test
    void testSetupIdmanExistsFail() {
        val testService = testService();
        doReturn(Optional.of(testService)).when(serviceStore).get("IDMAN");
        val r = firstTimeWizard.setup(URI.create("idmantest.com"), "a@a.com", "SS", "xx");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
    }

    @Test
    void testSetupOtherFail() {
        doReturn(Optional.empty()).when(serviceStore).get("IDMAN");
        try {
            val r = firstTimeWizard.setup(URI.create("idmantest.com"), "a@a.com", "SS", "xx");
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
    }

    @Test
    void testSetupSuccess() {
        val testService = testService();
        testService.setServiceId("IDMAN");
        val adminUser = TestingUtils.adminUser();
        val adminRole = new StoredRole("IDMAN_ADMIN", testService.getServiceId(), "Admin", "Admin role");
        val userRole = new StoredRole("IDMAN_USER", testService.getServiceId(), "User", "General user");
        doReturn(Optional.empty())
                .when(serviceStore)
                .get("IDMAN");
//        doReturn(Optional.of(testService))
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                val callback = invocation.getArgument(2, String.class);
                testService.setCallbackUrl(callback);
                return Optional.of(testService);
            }
        })
                .when(serviceStore)

                .create(eq("IDMan"),
                        anyString(),
                        anyString());
        doReturn(Optional.of(adminRole))
                .when(roleStore)
                .create(eq(testService.getServiceId()),
                        eq("Admin"),
                        anyString());
        doReturn(Optional.of(userRole))
                .when(roleStore)
                .create(eq(testService.getServiceId()),
                        eq("User"),
                        anyString());
        doReturn(Optional.of(adminUser))
                .when(userInfoStore)
                .create(anyString(),
                        eq(adminUser.getEmail()),
                        eq(adminUser.getName()),
                        eq(UserType.HUMAN),
                        eq(AuthMode.PASSWORD),
                        eq(false));
        val r = firstTimeWizard.setup(URI.create("https://idmantest.com/setup"),
                                      adminUser.getEmail(),
                                      adminUser.getName(),
                                      "xx");
        assertEquals(HttpStatus.SC_SEE_OTHER, r.getStatus());
        assertEquals(URI.create("/"), r.getLocation());
        assertEquals("https://idmantest.com/apis/idman/auth/callback",
                     testService.getCallbackUrl());
    }
}