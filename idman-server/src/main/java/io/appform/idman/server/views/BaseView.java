package io.appform.idman.server.views;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.vyarus.guicey.gsp.views.template.TemplateView;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class BaseView extends TemplateView {
    private final String error;

    public BaseView(String templatePath, String error) {
        super(templatePath);
        this.error = error;
    }
}
