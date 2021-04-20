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
public class ServiceDetailsView extends TemplateView {
    @Value
    public static class ServiceUser {
        StoredUser user;
        StoredRole role;
    }
    StoredService service;
    List<StoredRole> roles;
    List<StoredUser> users;
    List<ServiceUser> mappedUsers;
    IdmanUser sessionUser;

    public ServiceDetailsView(
            StoredService service,
            List<StoredRole> roles,
            List<StoredUser> users, List<ServiceUser> mappedUsers,
            IdmanUser sessionUser) {
        super("templates/servicedetails.hbs");
        this.service = service;
        this.roles = roles;
        this.users = users;
        this.mappedUsers = mappedUsers;
        this.sessionUser = sessionUser;
    }
}
