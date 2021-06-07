package io.appform.idman.server.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.StringTemplateSource;
import com.google.common.collect.ImmutableMap;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.auth.IdmanRoles;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static io.appform.idman.server.utils.ServerTestingUtils.adminUser;
import static io.appform.idman.server.utils.ServerTestingUtils.normalUser;
import static io.appform.idman.server.utils.Utils.toWire;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class CustomHelpersTest {
    static final Handlebars HANDLEBARS = new Handlebars().registerHelpers(new CustomHelpers());

    @Test
    @SneakyThrows
    void testAdmin() {
        val c = HANDLEBARS.compile(new StringTemplateSource("test", "{{#admin user}}true{{else}}false{{/admin}}"));
        assertEquals("false",
                     c.apply(Collections.singletonMap("user",
                                                      new IdmanUser("s", "S", toWire(adminUser()), IdmanRoles.USER))));
        assertEquals("false", c.apply(Collections.emptyMap()));

        assertEquals("false",
                     c.apply(Collections.singletonMap("user",
                                                      new IdmanUser("s", "S", toWire(normalUser()), null))));
        assertEquals("true",
                     c.apply(Collections.singletonMap("user",
                                                      new IdmanUser("s", "S", toWire(normalUser()), IdmanRoles.ADMIN))));
    }

    @Test
    @SneakyThrows
    void testAdminOrSelf() {
        val c = HANDLEBARS.compile(new StringTemplateSource("test", "{{#adminOrSelf user user2}}true{{else}}false{{/adminOrSelf}}"));
        val admin = adminUser();
        val normalUser = normalUser();
        assertEquals("true",
                     c.apply(
                             ImmutableMap.of("user",
                                             new IdmanUser("s", "S", toWire(admin), IdmanRoles.ADMIN),
                                             "user2", normalUser)
                            ));
        assertEquals("true",
                     c.apply(
                             ImmutableMap.of("user",
                                             new IdmanUser("s", "S", toWire(normalUser), IdmanRoles.USER),
                                             "user2", normalUser)
                            ));
        assertEquals("false",
                     c.apply(
                             ImmutableMap.of("user",
                                             new IdmanUser("s", "S", toWire(normalUser), IdmanRoles.USER),
                                             "user2", admin)
                            ));
        assertEquals("false",
                     c.apply(
                             ImmutableMap.of("user",
                                             new IdmanUser("s", "S", toWire(normalUser), IdmanRoles.USER))
                            ));
        assertEquals("false",
                     c.apply(ImmutableMap.of("user2", normalUser)));
        assertEquals("false",
                     c.apply(
                             ImmutableMap.of("user",
                                             new IdmanUser("s", "S", toWire(normalUser), null))
                            ));


    }

}