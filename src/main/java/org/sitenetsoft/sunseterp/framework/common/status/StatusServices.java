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
package org.sitenetsoft.sunseterp.framework.common.status;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.util.*;

import static org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics.checkCollection;

/**
 * StatusServices
 */
public class StatusServices {

    private static final String MODULE = StatusServices.class.getName();
    private static final String RESOURCE = "CommonUiLabels";

    public static Map<String, Object> getStatusItems(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        List<String> statusTypes = checkCollection(context.get("statusTypeIds"), String.class);
        Locale locale = (Locale) context.get("locale");
        if (UtilValidate.isEmpty(statusTypes)) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "CommonStatusMandatory", locale));
        }

        List<GenericValue> statusItems = new LinkedList<>();
        for (String statusTypeId: statusTypes) {
            try {
                List<GenericValue> myStatusItems = EntityQuery.use(delegator)
                                                              .from("StatusItem")
                                                              .where("statusTypeId", statusTypeId)
                                                              .orderBy("sequenceId")
                                                              .cache(true)
                                                              .queryList();
                statusItems.addAll(myStatusItems);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }
        }
        Map<String, Object> ret = new LinkedHashMap<>();
        ret.put("statusItems", statusItems);
        return ret;
    }

    public static Map<String, Object> getStatusValidChangeToDetails(DispatchContext ctx, Map<String, ?> context) {
        Delegator delegator = ctx.getDelegator();
        List<GenericValue> statusValidChangeToDetails = null;
        String statusId = (String) context.get("statusId");
        try {
            statusValidChangeToDetails = EntityQuery.use(delegator)
                                                    .from("StatusValidChangeToDetail")
                                                    .where("statusId", statusId)
                                                    .orderBy("sequenceId")
                                                    .cache(true)
                                                    .queryList();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        Map<String, Object> ret = ServiceUtil.returnSuccess();
        if (statusValidChangeToDetails != null) {
            ret.put("statusValidChangeToDetails", statusValidChangeToDetails);
        }
        return ret;
    }
}
