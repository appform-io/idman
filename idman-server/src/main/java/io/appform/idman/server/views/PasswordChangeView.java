package io.appform.idman.server.views;

import io.appform.idman.model.IdmanUser;
import io.appform.idman.server.db.model.StoredUser;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PasswordChangeView extends TemplateView {
    StoredUser user;
    boolean skipOld;
    IdmanUser sessionUser;
    public PasswordChangeView(StoredUser user, boolean skipOld, IdmanUser sessionUser) {
        super("templates/changepassword.hbs");
        this.user = user;
        this.skipOld = skipOld;
        this.sessionUser = sessionUser;
    }
}
