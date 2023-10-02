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
package org.sitenetsoft.framework.testtools;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.sitenetsoft.framework.base.component.ComponentConfig;
import org.sitenetsoft.framework.base.config.GenericConfigException;
import org.sitenetsoft.framework.base.config.ResourceHandler;
import org.sitenetsoft.framework.base.util.Debug;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.LinkedList;
import java.util.List;

/**
 * Use this class in a JUnit test runner to prepare the TestSuite.
 */
public class JunitSuiteWrapper {

    private static final String MODULE = JunitSuiteWrapper.class.getName();
    private List<ModelTestSuite> modelTestSuiteList = new LinkedList<>();

    public JunitSuiteWrapper(String componentName, String suiteName, String testCase) {
        for (ComponentConfig.TestSuiteInfo testSuiteInfo: ComponentConfig.getAllTestSuiteInfos(componentName)) {
            ResourceHandler testSuiteResource = testSuiteInfo.createResourceHandler();

            try {
                Document testSuiteDocument = testSuiteResource.getDocument();
                // TODO create TestSuite object based on this that will contain its TestCase objects

                Element documentElement = testSuiteDocument.getDocumentElement();
                ModelTestSuite modelTestSuite = new ModelTestSuite(documentElement, testCase);

                // make sure there are test-cases configured for the suite
                if (suiteName != null && !modelTestSuite.getSuiteName().equals(suiteName)) {
                    continue;
                }
                if (modelTestSuite.getTestList().size() > 0) {
                    this.modelTestSuiteList.add(modelTestSuite);
                }
            } catch (GenericConfigException e) {
                String errMsg = "Error reading XML document from ResourceHandler for loader [" + testSuiteResource.getLoaderName()
                        + "] and location [" + testSuiteResource.getLocation() + "]";
                Debug.logError(e, errMsg, MODULE);
            }
        }
    }

    /**
     * Populate test suite.
     * @param suite the suite
     */
    @Deprecated
    public void populateTestSuite(TestSuite suite) {
        for (ModelTestSuite modelTestSuite: this.modelTestSuiteList) {
            for (Test tst: modelTestSuite.getTestList()) {
                suite.addTest(tst);
            }
        }
    }

    /**
     * Gets model test suites.
     * @return the model test suites
     */
    public List<ModelTestSuite> getModelTestSuites() {
        return this.modelTestSuiteList;
    }

    /**
     * Gets all test list.
     * @return the all test list
     */
    public List<Test> getAllTestList() {
        List<Test> allTestList = new LinkedList<>();

        for (ModelTestSuite modelTestSuite: this.modelTestSuiteList) {
            for (Test tst: modelTestSuite.getTestList()) {
                allTestList.add(tst);
            }
        }

        return allTestList;
    }
}
