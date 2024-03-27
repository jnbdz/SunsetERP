package org.sitenetsoft.sunseterp.framework.webapp.view;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.webapp.control.ControlFilter;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

import java.io.IOException;

public class QuteViewHandler extends AbstractViewHandler {

    private static final String MODULE = QuteViewHandler.class.getName();
    private ServletContext context;

    @Inject
    Template pageTemplate;

    @Override
    public void init(ServletContext context) throws ViewHandlerException {
        this.context = context;
    }

    @Override
    public void render(
            String name,
            String page,
            String contentType,
            String encoding,
            String info,
            HttpServletRequest request,
            HttpServletResponse response) throws ViewHandlerException {
        if (request == null) {
            throw new ViewHandlerException("Null HttpServletRequest object");
        }
        if (UtilValidate.isEmpty(page)) {
            throw new ViewHandlerException("Null or empty source");
        }

        Debug.logInfo("Requested Page : " + page, MODULE);

        // tell the ControlFilter we are forwarding
        request.setAttribute(ControlFilter.FORWARDED_FROM_SERVLET, Boolean.TRUE);

        // Create a TemplateInstance
        TemplateInstance instance = pageTemplate.instance(); // specify the template name here

        try {
            String renderedTemplate = instance.render();
            response.getWriter().write(renderedTemplate);
        } catch (IOException ie) {
            throw new ViewHandlerException("IO Error in view", ie);
        }
    }
}
