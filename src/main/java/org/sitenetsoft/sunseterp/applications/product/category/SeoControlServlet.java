/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.sitenetsoft.sunseterp.applications.product.category;

// TODO: Use Quarkus to handle this with Qute templates
//import org.apache.catalina.servlets.DefaultServlet;
//import org.apache.jasper.servlet.JspServlet;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.webapp.SeoConfigUtil;
import org.sitenetsoft.sunseterp.framework.webapp.control.ControlServlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Locale;

/**
 * SeoControlServlet.java - SEO Master servlet for the web application.
 */
@SuppressWarnings("serial")
public class SeoControlServlet extends ControlServlet {

    private static String defaultPage = null;
    private static String controlServlet = null;
    public static final String REQUEST_IN_ALLOW_LIST = "_REQUEST_IN_ALLOW_LIST_";

    @Inject
    Template defaultTemplate;

    public SeoControlServlet() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = this.getServletContext();
        if (UtilValidate.isEmpty(SeoControlServlet.getDefaultPage())) {
            setDefaultPage(context.getInitParameter("defaultPage"));
        }
        if (UtilValidate.isEmpty(getDefaultPage())) {
            setDefaultPage("/main");
        }

        if (getDefaultPage().startsWith("/") && getDefaultPage().lastIndexOf('/') > 0) {
            setControlServlet(getDefaultPage().substring(1));
            setControlServlet(getControlServlet().substring(0, getControlServlet().indexOf('/')));
        }

        SeoConfigUtil.init();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TemplateInstance instance = defaultTemplate.instance();
        try {
            String renderedTemplate = instance.render();
            response.getWriter().write(renderedTemplate);
        } catch (IOException ie) {
            throw new ServletException("IO Error in view", ie);
        }
    }

    public static String getDefaultPage() {
        return defaultPage;
    }

    public static void setDefaultPage(String defaultPage) {
        SeoControlServlet.defaultPage = defaultPage;
    }

    public static String getControlServlet() {
        return controlServlet;
    }

    public static void setControlServlet(String controlServlet) {
        SeoControlServlet.controlServlet = controlServlet;
    }

}
