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

package org.sitenetsoft.sunseterp.resource.content.content;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilHttp;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.common.UrlServletHelper;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.webapp.WebAppUtil;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ContentUrlFilter implements Filter {
    private static final String MODULE = ContentUrlFilter.class.getName();
    private FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = (Delegator) httpRequest.getSession().getServletContext().getAttribute("delegator");

        String urlContentId = null;
        String pathInfo = UtilHttp.getFullRequestUrl(httpRequest);
        if (UtilValidate.isNotEmpty(pathInfo)) {
            String alternativeUrl = pathInfo.substring(pathInfo.lastIndexOf('/'));
            if (alternativeUrl.endsWith("-content")) {
                try {
                    GenericValue contentDataResourceView = EntityQuery.use(delegator).from("ContentDataResourceView")
                            .where("drObjectInfo", alternativeUrl)
                            .orderBy("createdDate DESC").queryFirst();
                    if (contentDataResourceView != null) {
                        GenericValue content = EntityQuery.use(delegator).from("ContentAssoc")
                                .where("contentAssocTypeId", "ALTERNATIVE_URL",
                                        "contentIdTo", contentDataResourceView.get("contentId"))
                                .filterByDate().queryFirst();
                        if (content != null) {
                            urlContentId = content.getString("contentId");
                        }
                    }
                } catch (GenericEntityException gee) {
                    Debug.logWarning(gee.getMessage(), MODULE);
                }
            }
            if (UtilValidate.isNotEmpty(urlContentId)) {
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append("/" + WebAppUtil.CONTROL_MOUNT_POINT);
                urlBuilder.append("/" + config.getInitParameter("viewRequest") + "?contentId=" + urlContentId);

                //Set view query parameters
                UrlServletHelper.setViewQueryParameters(request, urlBuilder);
                Debug.logInfo("[Filtered request]: " + pathInfo + " (" + urlBuilder + ")", MODULE);
                RequestDispatcher dispatch = request.getRequestDispatcher(urlBuilder.toString());
                dispatch.forward(request, response);
                return;
            }

            //Check path alias
            UrlServletHelper.checkPathAlias(request, httpResponse, delegator, pathInfo);
        }
        // we're done checking; continue on
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
