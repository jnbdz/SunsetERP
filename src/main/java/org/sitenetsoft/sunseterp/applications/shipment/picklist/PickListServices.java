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
package org.sitenetsoft.sunseterp.applications.shipment.picklist;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PickListServices {

    private static final String MODULE = PickListServices.class.getName();

    public static Map<String, Object> convertOrderIdListToHeaders(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();

        List<GenericValue> orderHeaderList = UtilGenerics.cast(context.get("orderHeaderList"));
        List<String> orderIdList = UtilGenerics.cast(context.get("orderIdList"));

        // we don't want to process if there is already a header list
        if (orderHeaderList == null) {
            // convert the ID list to headers
            if (orderIdList != null) {
                List<EntityCondition> conditionList1 = new LinkedList<>();
                List<EntityCondition> conditionList2 = new LinkedList<>();

                // we are only concerned about approved sales orders
                conditionList2.add(EntityCondition.makeCondition("statusId", EntityOperator.EQUALS, "ORDER_APPROVED"));
                conditionList2.add(EntityCondition.makeCondition("orderTypeId", EntityOperator.EQUALS, "SALES_ORDER"));

                // build the expression list from the IDs
                for (String orderId: orderIdList) {
                    conditionList1.add(EntityCondition.makeCondition("orderId", EntityOperator.EQUALS, orderId));
                }

                // create the conditions
                EntityCondition idCond = EntityCondition.makeCondition(conditionList1, EntityOperator.OR);
                conditionList2.add(idCond);

                // run the query
                try {
                    orderHeaderList = EntityQuery.use(delegator).from("OrderHeader")
                            .where(conditionList2)
                            .orderBy("orderDate")
                            .queryList();
                } catch (GenericEntityException e) {
                    Debug.logError(e, MODULE);
                    return ServiceUtil.returnError(e.getMessage());
                }
                Debug.logInfo("Recieved orderIdList  - " + orderIdList, MODULE);
                Debug.logInfo("Found orderHeaderList - " + orderHeaderList, MODULE);
            }
        }

        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("orderHeaderList", orderHeaderList);
        return result;
    }

    public static boolean isBinComplete(Delegator delegator, String picklistBinId) throws GeneralException {
        try {
            EntityCondition cond = EntityCondition.makeCondition(
                    EntityCondition.makeCondition("itemStatusId", EntityOperator.NOT_IN, UtilMisc.toList("PICKITEM_COMPLETED", "PICKITEM_CANCELLED")),
                    EntityCondition.makeCondition("picklistBinId", picklistBinId));
            long picklistItemCount = EntityQuery.use(delegator).from("PicklistItem").where(cond).queryCount();
            if (picklistItemCount != 0) {
                return false;
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            throw e;
        }

        return true;
    }
}
