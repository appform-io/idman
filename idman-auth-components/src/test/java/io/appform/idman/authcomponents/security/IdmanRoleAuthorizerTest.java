package io.appform.idman.authcomponents.security;

import io.appform.idman.model.AuthMode;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.model.User;
import io.appform.idman.model.UserType;
import lombok.val;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class IdmanRoleAuthorizerTest {

    private IdmanRoleAuthorizer authorizer = new IdmanRoleAuthorizer();

    @Test
    void testAuthorization() {
        val p = new ServiceUserPrincipal(new IdmanUser("S1",
                                                       "S",
                                                       new User("U1", "U", UserType.HUMAN, AuthMode.PASSWORD),
                                                       "R"));
        assertTrue(authorizer.authorize(p, "R"));
        assertFalse(authorizer.authorize(p, "R1"));
    }
}