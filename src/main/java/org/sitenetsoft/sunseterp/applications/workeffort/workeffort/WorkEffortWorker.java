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
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/** WorkEffortWorker - Work Effort worker class. */
public final class WorkEffortWorker {

    private static final String MODULE = WorkEffortWorker.class.getName();

    private WorkEffortWorker() { }

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId) {
        return getLowestLevelWorkEfforts(delegator, workEffortId, workEffortAssocTypeId, "workEffortIdFrom", "workEffortIdTo");
    }

    public static List<GenericValue> getLowestLevelWorkEfforts(Delegator delegator, String workEffortId, String workEffortAssocTypeId,
                                                               String left, String right) {
        if (left == null) {
            left = "workEffortIdFrom";
        }
        if (right == null) {
            right = "workEffortIdTo";
        }

        List<GenericValue> workEfforts = new LinkedList<>();
        try {
            List<GenericValue> childWEAssocsLevelFirst = EntityQuery.use(delegator).from("WorkEffortAssoc").where(left, workEffortId,
                    "workEffortAssocTypeId", workEffortAssocTypeId).cache(true).queryList();
            for (GenericValue childWEAssocLevelFirst : childWEAssocsLevelFirst) {
                List<GenericValue> childWEAssocsLevelNext = EntityQuery.use(delegator).from("WorkEffortAssoc").where(left,
                        childWEAssocLevelFirst.get(right), "workEffortAssocTypeId", workEffortAssocTypeId).cache(true).queryList();
                while (UtilValidate.isNotEmpty(childWEAssocsLevelNext)) {
                    List<GenericValue> tempWorkEffortList = new LinkedList<>();
                    for (GenericValue childWEAssocLevelNext : childWEAssocsLevelNext) {
                        List<GenericValue> childWEAssocsLevelNth = EntityQuery.use(delegator).from("WorkEffortAssoc").where(left,
                                childWEAssocLevelNext.get(right), "workEffortAssocTypeId", workEffortAssocTypeId).cache(true).queryList();
                        if (UtilValidate.isNotEmpty(childWEAssocsLevelNth)) {
                            tempWorkEffortList.addAll(childWEAssocsLevelNth);
                        }
                        workEfforts.add(childWEAssocLevelNext);
                    }
                    childWEAssocsLevelNext = tempWorkEffortList;
                }
                workEfforts.add(childWEAssocLevelFirst);
            }
        } catch (GenericEntityException e) {
            Debug.logWarning(e, MODULE);
        }
        return workEfforts;
    }

    public static List<GenericValue> removeDuplicateWorkEfforts(List<GenericValue> workEfforts) {
        Set<String> keys = new HashSet<>();
        Set<GenericValue> exclusions = new HashSet<>();
        for (GenericValue workEffort : workEfforts) {
            String workEffortId = workEffort.getString("workEffortId");
            if (keys.contains(workEffortId)) {
                exclusions.add(workEffort);
            } else {
                keys.add(workEffortId);
            }
        }
        workEfforts.removeAll(exclusions);
        return workEfforts;
    }
}
