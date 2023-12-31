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
package org.sitenetsoft.sunseterp.framework.testtools;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.security.Security;
import org.sitenetsoft.sunseterp.framework.security.SecurityConfigurationException;
import org.sitenetsoft.sunseterp.framework.security.SecurityFactory;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.testtools.OFBizTestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/*public class SimpleMethodTest extends OFBizTestCase {

    private static final String MODULE = ServiceTest.class.getName();
    private String methodLocation;
    private String methodName;
    private static MockHttpServletRequest request = new MockHttpServletRequest();
    private static MockHttpServletResponse response = new MockHttpServletResponse();


    public SimpleMethodTest(String caseName, Element mainElement) {
        this(caseName, mainElement.getAttribute("location"), mainElement.getAttribute("name"));
    }

    public SimpleMethodTest(String caseName, String methodLocation, String methodName) {
        super(caseName);
        this.methodLocation = methodLocation;
        this.methodName = methodName;
    }
}*/

public class SimpleMethodTest extends OFBizTestCase {

    private static final String MODULE = ServiceTest.class.getName();

    private String methodLocation;
    private String methodName;
    private static MockHttpServletRequest request = new MockHttpServletRequest();
    private static MockHttpServletResponse response = new MockHttpServletResponse();

    /**
     * Tests of Simple Method
     * @param caseName test case name
     * @param mainElement DOM main element
     */
    public SimpleMethodTest(String caseName, Element mainElement) {
        this(caseName, mainElement.getAttribute("location"), mainElement.getAttribute("name"));
    }

    public SimpleMethodTest(String caseName, String methodLocation, String methodName) {
        super(caseName);
        this.methodLocation = methodLocation;
        this.methodName = methodName;
    }

    @Override
    public int countTestCases() {
        return 1;
    }

    @Override
    public void run(TestResult result) {
        result.startTest(this);
        try {
            // define request
            Security security = SecurityFactory.getInstance(getDelegator());
            MockServletContext servletContext = new MockServletContext();
            request.setAttribute("security", security);
            request.setAttribute("servletContext", servletContext);
            request.setAttribute("delegator", getDelegator());
            request.setAttribute("dispatcher", getDispatcher());
            Map<String, Object> serviceResult = SimpleMethod.runSimpleService(methodLocation, methodName, getDispatcher().getDispatchContext(),
                    UtilMisc.toMap("test", this, "testResult", result, "locale", Locale.getDefault(), "request", request, "response", response));

            // do something with the errorMessage
            String errorMessage = (String) serviceResult.get(ModelService.ERROR_MESSAGE);
            if (UtilValidate.isNotEmpty(errorMessage)) {
                result.addFailure(this, new AssertionFailedError(errorMessage));
            }

            // do something with the errorMessageList
            List<Object> errorMessageList = UtilGenerics.cast(serviceResult.get(ModelService.ERROR_MESSAGE_LIST));
            if (UtilValidate.isNotEmpty(errorMessageList)) {
                for (Object message: errorMessageList) {
                    result.addFailure(this, new AssertionFailedError(message.toString()));
                }
            }

            // do something with the errorMessageMap
            Map<String, Object> errorMessageMap = UtilGenerics.cast(serviceResult.get(ModelService.ERROR_MESSAGE_MAP));
            if (!UtilValidate.isEmpty(errorMessageMap)) {
                for (Map.Entry<String, Object> entry: errorMessageMap.entrySet()) {
                    result.addFailure(this, new AssertionFailedError(entry.getKey() + ": " + entry.getValue()));
                }
            }

        } catch (MiniLangException | SecurityConfigurationException e) {
            result.addError(this, e);
        }

        result.endTest(this);
    }
}