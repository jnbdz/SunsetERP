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
import org.sitenetsoft.sunseterp.framework.base.location.FlexibleLocation;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityDataAssert;
import org.sitenetsoft.sunseterp.framework.entity.util.EntitySaxReader;
import org.sitenetsoft.sunseterp.framework.service.testtools.OFBizTestCase;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class EntityXmlAssertTest extends OFBizTestCase {

    private static final String MODULE = ServiceTest.class.getName();

    private String entityXmlUrlString;
    private String action;

    /**
     * Tests of entity xml
     * @param caseName test case name
     * @param mainElement DOM main element
     */
    public EntityXmlAssertTest(String caseName, Element mainElement) {
        super(caseName);
        this.entityXmlUrlString = mainElement.getAttribute("entity-xml-url");
        this.action = mainElement.getAttribute("action");
        if (UtilValidate.isEmpty(this.action)) this.action = "assert";
    }

    @Override
    public int countTestCases() {
        int testCaseCount = 0;
        try {
            URL entityXmlURL = FlexibleLocation.resolveLocation(entityXmlUrlString);
            EntitySaxReader reader = new EntitySaxReader(getDelegator());
            testCaseCount += reader.parse(entityXmlURL);
        } catch (Exception e) {
            Debug.logError(e, "Error getting test case count", MODULE);
        }
        return testCaseCount;
    }

    @Override
    public void run(TestResult result) {
        result.startTest(this);

        try {
            URL entityXmlURL = FlexibleLocation.resolveLocation(entityXmlUrlString);
            List<Object> errorMessages = new LinkedList<>();

            if ("assert".equals(this.action)) {
                EntityDataAssert.assertData(entityXmlURL, getDelegator(), errorMessages);
            } else if ("load".equals(this.action)) {
                EntitySaxReader reader = new EntitySaxReader(getDelegator());
                reader.parse(entityXmlURL);
            } else {
                // uh oh, bad value
                result.addFailure(this, new AssertionFailedError("Bad value [" + this.action + "] for action attribute of entity-xml element"));
            }

            if (UtilValidate.isNotEmpty(errorMessages)) {
                for (Object failureMessage: errorMessages) {
                    result.addFailure(this, new AssertionFailedError(failureMessage.toString()));
                }
            }
        } catch (Exception e) {
            result.addError(this, e);
        }

        result.endTest(this);
    }
}
