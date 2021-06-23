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
public class ErrorView extends TemplateView {
    String errorCode;
    String errorDescription;
    public ErrorView(String errorCode, String errorDescription) {
        super("templates/login_error.hbs");
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
}
