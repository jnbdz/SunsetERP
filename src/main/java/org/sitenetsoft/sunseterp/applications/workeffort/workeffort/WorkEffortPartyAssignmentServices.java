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

package org.sitenetsoft.sunseterp.applications.workeffort.workeffort;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.util.Map;

/**
 * WorkEffortPartyAssignmentServices - Services to handle form input and other data changes.
 */
public class WorkEffortPartyAssignmentServices {

    private static final String MODULE = WorkEffortPartyAssignmentServices.class.getName();

    public static void updateWorkflowEngine(GenericValue wepa, GenericValue userLogin, LocalDispatcher dispatcher) {
        // if the WorkEffort is an ACTIVITY, check for accept or complete new status...
        Delegator delegator = wepa.getDelegator();
        GenericValue workEffort = null;

        try {
            workEffort = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", wepa.get("workEffortId")).queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        if (workEffort != null && "ACTIVITY".equals(workEffort.getString("workEffortTypeId"))) {
            // TODO: restrict status transitions

            String statusId = (String) wepa.get("statusId");
            Map<String, Object> context = UtilMisc.toMap("workEffortId", wepa.get("workEffortId"), "partyId", wepa.get("partyId"),
                    "roleTypeId", wepa.get("roleTypeId"), "fromDate", wepa.get("fromDate"),
                    "userLogin", userLogin);

            if ("CAL_ACCEPTED".equals(statusId)) {
                // accept the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfAcceptAssignment", context);
                    if (ServiceUtil.isError(results)) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            } else if ("CAL_COMPLETED".equals(statusId)) {
                // complete the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfCompleteAssignment", context);
                    if (ServiceUtil.isError(results)) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            } else if ("CAL_DECLINED".equals(statusId)) {
                // decline the activity assignment
                try {
                    Map<String, Object> results = dispatcher.runSync("wfDeclineAssignment", context);

                    if (results != null && results.get(ModelService.ERROR_MESSAGE) != null) {
                        Debug.logWarning((String) results.get(ModelService.ERROR_MESSAGE), MODULE);
                    }
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }
    }
}
