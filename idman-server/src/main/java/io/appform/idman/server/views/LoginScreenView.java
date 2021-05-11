package io.appform.idman.server.views;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 *
 */
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LoginScreenView extends BaseView {

    String serviceId;
    String clientSessionId;
    String redirect;

    public LoginScreenView(String error, String serviceId, String clientSessionId, String redirect) {
        super("templates/loginscreen.hbs", error);
        this.serviceId = serviceId;
        this.clientSessionId = clientSessionId;
        this.redirect = redirect;
    }
}
