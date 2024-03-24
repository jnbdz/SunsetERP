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
package org.sitenetsoft.sunseterp.applications.manufacturing.jobshopmgt;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Helper for Production Run maintenance
 *
 */
public final class ProductionRunHelper {

    private static final String MODULE = ProductionRunHelper.class.getName();

    private ProductionRunHelper() { }

    /**
     * Get a Production Run.
     * Check if routing - product link exists.
     * @param delegator the delegator
     * @param productionRunId the production run id
     * @return Map with the result of the service, the output parameters are
     */
    public static Map<String, Object> getProductionRun(Delegator delegator, String productionRunId) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (productionRunId != null) {
                GenericValue productionRun = EntityQuery.use(delegator).from("WorkEffort").where("workEffortId", productionRunId).queryOne();
                if (productionRun != null) {
                    List<GenericValue> productionRunProducts = productionRun.getRelated("WorkEffortGoodStandard",
                            UtilMisc.toMap("workEffortGoodStdTypeId", "PRUN_PROD_DELIV"), null, false);
                    GenericValue productionRunProduct = EntityUtil.getFirst(productionRunProducts);
                    GenericValue productProduced = productionRunProduct.getRelatedOne("Product", true);
                    List<GenericValue> productionRunComponents = productionRun.getRelated("WorkEffortGoodStandard",
                            UtilMisc.toMap("workEffortGoodStdTypeId", "PRUNT_PROD_NEEDED"), null, false);
                    List<GenericValue> productionRunRoutingTasks = productionRun.getRelated("FromWorkEffortAssoc",
                            UtilMisc.toMap("workEffortTypeId", "PROD_ORDER_TASK"), null, false);
                    result.put("productionRunProduct", productionRunProduct);
                    result.put("productProduced", productProduced);
                    result.put("productionRunComponents", productionRunComponents);
                    result.put("productionRunRoutingTasks", productionRunRoutingTasks);
                }
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
        }
        return result;
    }

    public static boolean hasTask(Delegator delegator, String taskName, String workEffortId) throws GenericEntityException {
        List<GenericValue> tasks = EntityQuery.use(delegator).from("WorkEffort")
                .where("workEffortParentId", workEffortId,
                        "workEffortTypeId", "PROD_ORDER_TASK",
                        "workEffortName", taskName)
                .queryList();
        return (UtilValidate.isNotEmpty(tasks));
    }

    public static void getLinkedProductionRuns(Delegator delegator, LocalDispatcher dispatcher, String productionRunId,
                                               List<ProductionRun> productionRuns) throws GenericEntityException {
        productionRuns.add(new ProductionRun(productionRunId, delegator, dispatcher));
        List<GenericValue> linkedWorkEfforts = EntityQuery.use(delegator).from("WorkEffortAssoc")
                .where("workEffortIdTo", productionRunId,
                        "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")
                .filterByDate().queryList();
        for (GenericValue link : linkedWorkEfforts) {
            getLinkedProductionRuns(delegator, dispatcher, link.getString("workEffortIdFrom"), productionRuns);
        }
    }

    public static String getRootProductionRun(Delegator delegator, String productionRunId) throws GenericEntityException {
        GenericValue linkedWorkEffort = EntityQuery.use(delegator).from("WorkEffortAssoc")
                .where("workEffortIdFrom", productionRunId, "workEffortAssocTypeId", "WORK_EFF_PRECEDENCY")
                .queryFirst();
        if (linkedWorkEffort != null) {
            productionRunId = getRootProductionRun(delegator, linkedWorkEffort.getString("workEffortIdTo"));
        }
        return productionRunId;
    }

}
