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

package org.sitenetsoft.sunseterp.applications.accounting.period;

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PeriodServices {

    private static final String MODULE = PeriodServices.class.getName();
    private static final String RESOURCE = "AccountingUiLabels";

    /*
     * find the date of the last closed CustomTimePeriod, or, if none available, the
     * earliest date available of any CustomTimePeriod
     */
    public static Map<String, Object> findLastClosedDate(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        String organizationPartyId = (String) context.get("organizationPartyId"); // input parameters
        String periodTypeId = (String) context.get("periodTypeId");
        Timestamp findDate = (Timestamp) context.get("findDate");
        Locale locale = (Locale) context.get("locale");

        // default findDate to now
        if (findDate == null) {
            findDate = UtilDateTime.nowTimestamp();
        }

        Timestamp lastClosedDate = null; // return parameters
        GenericValue lastClosedTimePeriod = null;
        Map<String, Object> result = ServiceUtil.returnSuccess();

        try {
            // try to get the ending date of the most recent accounting time period before
            // findDate which has been closed
            List<EntityCondition> findClosedConditions = UtilMisc.toList(
                    EntityCondition.makeConditionMap("organizationPartyId", organizationPartyId),
                    EntityCondition.makeCondition("thruDate", EntityOperator.LESS_THAN_EQUAL_TO, findDate),
                    EntityCondition.makeConditionMap("isClosed", "Y"));
            if (UtilValidate.isNotEmpty(periodTypeId)) {
                // if a periodTypeId was supplied, use it
                findClosedConditions.add(EntityCondition.makeConditionMap("periodTypeId", periodTypeId));
            }
            GenericValue closedTimePeriod = EntityQuery.use(delegator).from("CustomTimePeriod")
                    .select("customTimePeriodId", "periodTypeId", "isClosed", "fromDate", "thruDate")
                    .where(findClosedConditions).orderBy("thruDate DESC").queryFirst();

            if (UtilValidate.isNotEmpty(closedTimePeriod) && UtilValidate.isNotEmpty(closedTimePeriod.get("thruDate"))) {
                lastClosedTimePeriod = closedTimePeriod;
                lastClosedDate = lastClosedTimePeriod.getTimestamp("thruDate");
            } else {
                // uh oh, no time periods have been closed? in that case, just find the earliest
                // beginning of a time period for this organization and optionally, for this period type
                Map<String, String> findParams = UtilMisc.toMap("organizationPartyId", organizationPartyId);
                if (UtilValidate.isNotEmpty(periodTypeId)) {
                    findParams.put("periodTypeId", periodTypeId);
                }
                GenericValue timePeriod = EntityQuery.use(delegator).from("CustomTimePeriod").where(findParams).orderBy("fromDate ASC").queryFirst();
                if (timePeriod != null && UtilValidate.isNotEmpty(timePeriod.get("fromDate"))) {
                    lastClosedDate = timePeriod.getTimestamp("fromDate");
                } else {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "AccountingPeriodCannotGet", locale));
                }
            }

            result.put("lastClosedTimePeriod", lastClosedTimePeriod); // ok if this is null - no time periods have been closed
            result.put("lastClosedDate", lastClosedDate); // should have a value - not null
            return result;
        } catch (GenericEntityException ex) {
            return (ServiceUtil.returnError(ex.getMessage()));
        }
    }
}
