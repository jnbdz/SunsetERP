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
package org.sitenetsoft.sunseterp.applications.product.store;

import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.applications.content.survey.SurveyWrapper;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;

import java.io.Writer;
import java.util.Map;

/**
 * Product Store Survey Wrapper
 */
public class ProductStoreSurveyWrapper extends SurveyWrapper {

    private static final String MODULE = ProductStoreSurveyWrapper.class.getName();

    private GenericValue productStoreSurveyAppl = null;
    private String surveyTemplate = null;
    private String resultTemplate = null;
    private boolean callResult = false;

    protected ProductStoreSurveyWrapper() { }

    public ProductStoreSurveyWrapper(GenericValue productStoreSurveyAppl, String partyId, Map<String, Object> passThru,
                                     Map<String, Object> defaultValues) {
        this.productStoreSurveyAppl = productStoreSurveyAppl;

        if (this.productStoreSurveyAppl != null) {
            this.setPartyId(partyId);
            this.setDelegator(productStoreSurveyAppl.getDelegator());
            this.setSurveyId(productStoreSurveyAppl.getString("surveyId"));
            this.surveyTemplate = productStoreSurveyAppl.getString("surveyTemplate");
            this.resultTemplate = productStoreSurveyAppl.getString("resultTemplate");
        } else {
            throw new IllegalArgumentException("Required parameter productStoreSurveyAppl missing");
        }
        this.setDefaultValues(defaultValues);
        // sanitize pass-thru, we need to remove hidden fields values that are set
        // by the survey so they won't be duplicated in additionalFields
        passThru.remove("surveyId");
        passThru.remove("partyId");
        passThru.remove("surveyResponseId");
        this.setPassThru(passThru);
        this.checkParameters();
    }

    public ProductStoreSurveyWrapper(GenericValue productStoreSurveyAppl, String partyId, Map<String, Object> passThru) {
        this(productStoreSurveyAppl, partyId, passThru, null);
    }

    /**
     * Call result.
     * @param b the b
     */
    public void callResult(boolean b) {
        this.callResult = b;
    }

    /**
     * Render writer.
     * @return the writer
     * @throws SurveyWrapperException the survey wrapper exception
     */
    public Writer render() throws SurveyWrapperException {
        if (canRespond() && !callResult) {
            return renderSurvey();
        } else if (UtilValidate.isNotEmpty(resultTemplate)) {
            return renderResult();
        } else {
            throw new SurveyWrapperException("Error template not implemented yet; cannot update survey; no result template defined!");
        }
    }

    /**
     * Render survey writer.
     * @return the writer
     * @throws SurveyWrapperException the survey wrapper exception
     */
    public Writer renderSurvey() throws SurveyWrapperException {
        return this.render(surveyTemplate);
    }

    /**
     * Render result writer.
     * @return the writer
     * @throws SurveyWrapperException the survey wrapper exception
     */
    public Writer renderResult() throws SurveyWrapperException {
        return this.render(resultTemplate);
    }
}
