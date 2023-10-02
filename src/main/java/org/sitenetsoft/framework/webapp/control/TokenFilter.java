/*
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
 */
package org.sitenetsoft.framework.webapp.control;

import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.UtilHttp;
import org.sitenetsoft.framework.base.util.UtilProperties;
import org.sitenetsoft.framework.base.util.UtilValidate;
import org.sitenetsoft.framework.common.CommonEvents;
import org.sitenetsoft.framework.entity.Delegator;
import org.sitenetsoft.framework.entity.GenericEntityException;
import org.sitenetsoft.framework.entity.GenericValue;
import org.sitenetsoft.framework.entity.util.EntityQuery;
import org.sitenetsoft.framework.service.ModelService;
import org.sitenetsoft.framework.webapp.WebAppUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class TokenFilter {
    private static final String MODULE = org.sitenetsoft.framework.webapp.control.TokenFilter.class.getName();
}

/*public class TokenFilter implements Filter {
    private static final String MODULE = TokenFilter.class.getName();

    private FilterConfig config = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Delegator delegator = WebAppUtil.getDelegator(config.getServletContext());
        Locale locale = UtilHttp.getLocale(httpRequest);

        String token = JWTManager.getHeaderAuthBearerToken(httpRequest);

        if (UtilValidate.isNotEmpty(token)) {
            Map<String, Object> result = JWTManager.validateToken(token, JWTManager.getJWTKey(delegator));
            String userLoginId = (String) result.get("userLoginId");
            if (UtilValidate.isNotEmpty(result.get(ModelService.ERROR_MESSAGE))) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpRequest.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("SecurityextUiLabels",
                        "loginservices.sorry_problem_processing_request_error", locale));
                CommonEvents.jsonResponseFromRequestAttributes(httpRequest, httpResponse);
            } else if (UtilValidate.isNotEmpty(userLoginId)) {
                try {
                    GenericValue userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", userLoginId).queryOne();
                    if (userLogin != null && !"N".equals(userLogin.getString("enabled"))) {
                        //FIXME: This is not good way for API, but session is required to get the userLogin while performing auth check
                        HttpSession session = httpRequest.getSession();
                        session.setAttribute("userLogin", userLogin);
                        chain.doFilter(httpRequest, httpResponse);
                    } else {
                        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        httpRequest.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("SecurityextUiLabels",
                                "loginservices.sorry_problem_processing_request_error", locale));
                        CommonEvents.jsonResponseFromRequestAttributes(httpRequest, httpResponse);
                    }
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    httpRequest.setAttribute("_ERROR_MESSAGE_", UtilProperties.getMessage("SecurityextUiLabels",
                            "loginservices.sorry_problem_processing_request_error_try_later", locale));
                    CommonEvents.jsonResponseFromRequestAttributes(httpRequest, httpResponse);
                }
            }
        } else {
            chain.doFilter(httpRequest, httpResponse);
        }
    }
    @Override
    public void destroy() {
        config = null;
    }
}*/
