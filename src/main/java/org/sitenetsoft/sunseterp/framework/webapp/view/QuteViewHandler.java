package org.sitenetsoft.sunseterp.framework.webapp.view;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class QuteViewHandler extends AbstractViewHandler {

    private static final String MODULE = QuteViewHandler.class.getName();
    private ServletContext context;

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

    }

}
