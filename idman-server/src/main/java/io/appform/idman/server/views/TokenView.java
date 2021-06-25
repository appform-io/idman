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
public class TokenView extends TemplateView {
    String token;
    String userId;

    public TokenView(String token, String userId) {
        super("templates/tokenpage.hbs");
        this.token = token;
        this.userId = userId;
    }
}
