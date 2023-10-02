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
package org.sitenetsoft.framework.webapp.control;

import org.sitenetsoft.framework.entity.util.EntityUtilProperties;

//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletResponse;
//import javax.ws.rs.core.HttpHeaders;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collection;


public class SameSiteFilter implements jakarta.servlet.Filter {

    private static final String SAME_SITE_COOKIE_ATTRIBUTE =
            EntityUtilProperties.getPropertyValueFromDelegatorName("security.properties", "SameSiteCookieAttribute", "strict", "default");
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        chain.doFilter(request, response);
        addSameSiteCookieAttribute((HttpServletResponse) response); // add SameSite=strict cookie attribute
    }

    public static void addSameSiteCookieAttribute(HttpServletResponse response) {
        Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
        boolean firstHeader = true;
        for (String header : headers) { // there can be multiple Set-Cookie attributes
            if (firstHeader) {
                response.setHeader(HttpHeaders.SET_COOKIE, String.format("%s; %s", header, "SameSite=" + SAME_SITE_COOKIE_ATTRIBUTE));
                firstHeader = false;
                continue;
            }
            response.addHeader(HttpHeaders.SET_COOKIE, String.format("%s; %s", header, "SameSite=" + SAME_SITE_COOKIE_ATTRIBUTE));
        }
    }

    @Override
    public void destroy() {

    }
}
