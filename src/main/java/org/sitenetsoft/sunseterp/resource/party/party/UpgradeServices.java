/*
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
 */

package org.sitenetsoft.sunseterp.resource.party.party;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.util.List;
import java.util.Map;

public class UpgradeServices {

    private static final String MODULE = UpgradeServices.class.getName();

    public static Map<String, Object> migrateMaritalStatusFromIndicatorToEnum(DispatchContext dctx, Map<String, Object> context) {
        Delegator delegator = dctx.getDelegator();
        try {
            List<GenericValue> persons = EntityQuery.use(delegator).from("Person")
                    .where(EntityCondition.makeCondition("oldMaritalStatus", EntityOperator.NOT_EQUAL, null)).queryList();
            for (GenericValue person : persons) {
                person.put("maritalStatusEnumId", "Y".equalsIgnoreCase(person.getString("oldMaritalStatus")) ? "MARRIED" : "SINGLE");
                person.store();
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(e.getMessage());
        }

        return ServiceUtil.returnSuccess();
    }
}
