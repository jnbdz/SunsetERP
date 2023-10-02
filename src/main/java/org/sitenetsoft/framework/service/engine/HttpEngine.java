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
package org.sitenetsoft.framework.service.engine;

import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.HttpClient;
import org.sitenetsoft.framework.base.util.HttpClientException;
import org.sitenetsoft.framework.base.util.UtilGenerics;
import org.sitenetsoft.framework.entity.Delegator;
import org.sitenetsoft.framework.entity.serialize.XmlSerializer;
import org.sitenetsoft.framework.service.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpEngine.java
 */
public class HttpEngine extends GenericAsyncEngine {

    private static final String MODULE = HttpEngine.class.getName();
    private static final boolean EXPORT_ALL = false;

    public HttpEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.framework.service.engine.GenericEngine#runSync(String, org.sitenetsoft.framework.service.ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        DispatchContext dctx = getDispatcher().getLocalContext(localName);
        String xmlContext = null;

        try {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Serializing Context --> " + context, MODULE);
            }
            xmlContext = XmlSerializer.serialize(context);
        } catch (Exception e) {
            throw new GenericServiceException("Cannot serialize context.", e);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("serviceName", modelService.getInvoke());
        if (xmlContext != null) {
            parameters.put("serviceContext", xmlContext);
        }

        HttpClient http = new HttpClient(this.getLocation(modelService), parameters);
        String postResult = null;
        try {
            postResult = http.post();
        } catch (HttpClientException e) {
            throw new GenericServiceException("Problems invoking HTTP request", e);
        }

        Map<String, Object> result = null;
        try {
            Object res = XmlSerializer.deserialize(postResult, dctx.getDelegator());
            if (res instanceof Map<?, ?>) {
                result = UtilGenerics.cast(res);
            } else {
                throw new GenericServiceException("Result not an instance of Map.");
            }
        } catch (Exception e) {
            throw new GenericServiceException("Problems deserializing result.", e);
        }

        return result;
    }

    /**
     * @see org.sitenetsoft.framework.service.engine.GenericEngine#runSyncIgnore(String, org.sitenetsoft.framework.service.ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }

    /**
     * Event for handling HTTP services
     * @param request HttpServletRequest object
     * @param response HttpServletResponse object
     * @return null
     */
    public static String httpEngine(HttpServletRequest request, HttpServletResponse response) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");

        String serviceName = request.getParameter("serviceName");
        String serviceMode = request.getParameter("serviceMode");
        String xmlContext = request.getParameter("serviceContext");

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> context = null;

        if (serviceName == null) {
            result.put(ModelService.ERROR_MESSAGE, "Cannot have null service name");
        }

        if (serviceMode == null) {
            serviceMode = "SYNC";
        }

        // deserialize the context
        if (!result.containsKey(ModelService.ERROR_MESSAGE)) {
            if (xmlContext != null) {
                try {
                    Object o = XmlSerializer.deserialize(xmlContext, delegator);
                    if (o instanceof Map<?, ?>) {
                        context = UtilGenerics.cast(o);
                    } else {
                        Debug.logError("Context not an instance of Map error", MODULE);
                        result.put(ModelService.ERROR_MESSAGE, "Context not an instance of Map");
                    }
                } catch (Exception e) {
                    Debug.logError(e, "Deserialization error", MODULE);
                    result.put(ModelService.ERROR_MESSAGE, "Error occurred deserializing context: " + e.toString());
                }
            }
        }

        // invoke the service
        if (!result.containsKey(ModelService.ERROR_MESSAGE)) {
            try {
                ModelService model = dispatcher.getDispatchContext().getModelService(serviceName);
                if (model.isExport() || EXPORT_ALL) {
                    if ("ASYNC".equals(serviceMode)) {
                        dispatcher.runAsync(serviceName, context);
                    } else {
                        result = dispatcher.runSync(serviceName, context);
                    }
                } else {
                    Debug.logWarning("Attempt to invoke a non-exported service: " + serviceName, MODULE);
                    throw new GenericServiceException("Cannot find requested service");
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Service invocation error", MODULE);
                result.put(ModelService.ERROR_MESSAGE, "Service invocation error: " + e.toString());
            }
        }

        // backup error message
        StringBuilder errorMessage = new StringBuilder();

        // process the result
        String resultString = null;
        try {
            resultString = XmlSerializer.serialize(result);
        } catch (Exception e) {
            Debug.logError(e, "Cannot serialize result", MODULE);
            if (result.containsKey(ModelService.ERROR_MESSAGE)) {
                errorMessage.append(result.get(ModelService.ERROR_MESSAGE));
            }
            errorMessage.append("::");
            errorMessage.append(e);
        }

        // handle the response
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("plain/text");

            if (errorMessage.length() > 0) {
                response.setContentLength(errorMessage.toString().getBytes("UTF-8").length);
                out.write(errorMessage.toString());
            } else {
                response.setContentLength(resultString.getBytes("UTF-8").length);
                out.write(resultString);
            }

            out.flush();
            response.flushBuffer();
        } catch (IOException e) {
            Debug.logError(e, "Problems w/ getting the servlet writer.", MODULE);
            return "error";
        }

        return null;
    }


}
