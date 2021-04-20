package io.appform.idman.server.views;

import io.appform.idman.model.IdmanUser;
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
public class HomeView extends TemplateView {
    List<StoredService> services;
    List<StoredUser> users;
    IdmanUser sessionUser;

    public HomeView(
            List<StoredService> services,
            List<StoredUser> users,
            IdmanUser sessionUser) {
        super("templates/home.hbs");
        this.services = services;
        this.users = users;
        this.sessionUser = sessionUser;
    }
}
