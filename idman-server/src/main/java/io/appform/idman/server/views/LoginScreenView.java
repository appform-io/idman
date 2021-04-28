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
public class LoginScreenView extends TemplateView {
    String serviceId;
    String clientSessionId;
    String redirect;

    public LoginScreenView(String serviceId, String clientSessionId, String redirect) {
        super("templates/loginscreen.hbs");
        this.serviceId = serviceId;
        this.clientSessionId = clientSessionId;
        this.redirect = redirect;
    }
}
