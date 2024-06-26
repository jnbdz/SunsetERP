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

import org.sitenetsoft.sunseterp.framework.common.UrlServletHelper;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.applications.product.category.ftl.CatalogUrlSeoTransform;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CatalogUrlSeoFilter extends CatalogUrlFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");

        // Get ServletContext
        ServletContext servletContext = getConfig().getServletContext();

        // Set request attribute and session
        UrlServletHelper.setRequestAttributes(request, delegator, servletContext);

        // set the ServletContext in the request for future use
        httpRequest.setAttribute("servletContext", getConfig().getServletContext());
        if (CatalogUrlSeoTransform.forwardUri(httpRequest, httpResponse, delegator, CategoryControlServlet.getControlServlet())) {
            return;
        }
        super.doFilter(httpRequest, httpResponse, chain);
    }

}
