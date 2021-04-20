package io.appform.idman.server.views;

import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.db.model.StoredRole;
import io.appform.idman.server.db.model.StoredService;
import io.appform.idman.server.db.model.StoredUser;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

import java.util.List;

/**
 * Renders the homepage
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserDetailsView extends TemplateView {

    @Value
    public static class UserServices {
        StoredService service;
        StoredRole role;
    }

    StoredUser user;
    List<UserServices> services;
    IdmanUser sessionUser;

    public UserDetailsView(
            StoredUser user,
            List<UserServices> services, IdmanUser sessionUser) {
        super("templates/userdetails.hbs");
        this.user = user;
        this.services = services;
        this.sessionUser = sessionUser;
    }
}
