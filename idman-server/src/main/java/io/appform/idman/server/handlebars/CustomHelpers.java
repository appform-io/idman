package io.appform.idman.server.handlebars;

import com.github.jknack.handlebars.Options;
import com.google.common.base.Objects;
import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.auth.IdmanRoles;
import io.appform.idman.server.db.model.StoredUser;

import java.io.IOException;

/**
 *
 */

public class CustomHelpers {
    public CharSequence eqstr(final Object obj1, final Options options) throws IOException {
        Object obj2 = options.param(0);
        return Objects.equal(obj1.toString(), obj2.toString()) ? options.fn() : options.inverse();
    }

    public CharSequence neqstr(final Object obj1, final Options options) throws IOException {
        Object obj2 = options.param(0);
        return !Objects.equal(obj1.toString(), obj2.toString()) ? options.fn() : options.inverse();
    }

    public CharSequence admin(final IdmanUser user, final Options options) throws IOException {
        return null != user && null != user.getRole() && user.getRole().equals(IdmanRoles.ADMIN)
                ? options.fn()
               : options.inverse();
    }

    public CharSequence adminOrSelf(final IdmanUser idmanUser, final StoredUser user, final Options options) throws IOException {
        return ((null != idmanUser && null != idmanUser.getRole() && idmanUser.getRole().equals(IdmanRoles.ADMIN))
                || (null != user && null != idmanUser && user.getUserId().equals(idmanUser.getUser().getId())))
                ? options.fn()
               : options.inverse();
    }
}
