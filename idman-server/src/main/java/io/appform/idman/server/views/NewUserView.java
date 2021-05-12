package io.appform.idman.server.views;

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
public class NewUserView extends TemplateView {
    private static final String TEMPALTE_VIEW = "templates/newuser.hbs";

    String actionPath;

    public NewUserView(String actionPath) {
        super(TEMPALTE_VIEW);
        this.actionPath = actionPath;
    }
}
