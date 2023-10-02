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
package org.sitenetsoft.framework.webapp.event;

/*
import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.UtilHttp;
import org.sitenetsoft.framework.base.util.UtilProperties;
import org.sitenetsoft.framework.entity.transaction.GenericTransactionException;
import org.sitenetsoft.framework.entity.transaction.TransactionUtil;
import org.sitenetsoft.framework.minilang.MiniLangException;
import org.sitenetsoft.framework.minilang.SimpleMethod;
import org.sitenetsoft.framework.webapp.control.ConfigXMLReader.Event;
import org.sitenetsoft.framework.webapp.control.ConfigXMLReader.RequestMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
*/

/**
 * SimpleEventHandler - Simple Event Mini-Lang Handler
 */
public class SimpleEventHandler {

    private static final String MODULE = org.sitenetsoft.framework.webapp.event.SimpleEventHandler.class.getName();
    /** Contains the property file name for translation of error messages. */
    private static final String ERR_RESOURCE = "WebappUiLabels";
}
/*public class SimpleEventHandler implements EventHandler {

    private static final String MODULE = SimpleEventHandler.class.getName();
    /** Contains the property file name for translation of error messages. */
    /*private static final String ERR_RESOURCE = "WebappUiLabels";

    @Override
    public void init(ServletContext context) throws EventHandlerException {
    }

    @Override
    public String invoke(Event event, RequestMap requestMap, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        boolean beganTransaction = false;

        String xmlResource = event.getPath();
        String eventName = event.getInvoke();
        Locale locale = UtilHttp.getLocale(request);

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Set path/method]: " + xmlResource + " / " + eventName, MODULE);
        }

        if (xmlResource == null) {
            throw new EventHandlerException("XML Resource (eventPath) cannot be null");
        }
        if (eventName == null) {
            throw new EventHandlerException("Event Name (eventMethod) cannot be null");
        }

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Processing]: SIMPLE Event", MODULE);
        }
        try {
            int timeout = Integer.max(event.getTransactionTimeout(), 0);
            beganTransaction = TransactionUtil.begin(timeout);
            String eventReturn = SimpleMethod.runSimpleEvent(xmlResource, eventName, request, response);
            if (Debug.verboseOn()) {
                Debug.logVerbose("[Event Return]: " + eventReturn, MODULE);
            }
            return eventReturn;
        } catch (MiniLangException e) {
            Debug.logError(e, MODULE);
            String errMsg = UtilProperties.getMessage(ERR_RESOURCE, "simpleEventHandler.event_not_completed", (
                    locale != null ? locale : Locale.getDefault())) + ": ";
            request.setAttribute("_ERROR_MESSAGE_", errMsg + e.getMessage());
            return "error";
        } catch (GenericTransactionException e) {
            Debug.logError(e, MODULE);
            return "error";
        } finally {
            try {
                TransactionUtil.commit(beganTransaction);
            } catch (GenericTransactionException e) {
                Debug.logError(e, MODULE);
            }
        }
    }
}*/