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
package org.sitenetsoft.framework.entityext;

import org.sitenetsoft.framework.base.util.Debug;
import org.sitenetsoft.framework.base.util.UtilValidate;
import org.sitenetsoft.framework.entity.GenericEntityException;
import org.sitenetsoft.framework.entity.GenericValue;
import org.sitenetsoft.framework.service.DispatchContext;
import org.sitenetsoft.framework.service.ServiceUtil;

import java.util.Map;

public class EntityWatchServices {

    private static final String MODULE = EntityWatchServices.class.getName();

    /**
     * This service is meant to be called through an Entity ECA (EECA) to watch an entity
     * @param dctx the dispatch context
     * @param context the context
     * @return the result of the service execution
     */
    public static Map<String, Object> watchEntity(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue newValue = (GenericValue) context.get("newValue");
        String fieldName = (String) context.get("fieldName");

        if (newValue == null) {
            return ServiceUtil.returnSuccess();
        }

        GenericValue currentValue = null;
        try {
            currentValue = dctx.getDelegator().findOne(newValue.getEntityName(), newValue.getPrimaryKey(), false);
        } catch (GenericEntityException e) {
            String errMsg = "Error finding currentValue for primary key [" + newValue.getPrimaryKey() + "]: " + e.toString();
            Debug.logError(e, errMsg, MODULE);
        }

        if (currentValue != null) {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object currentFieldValue = currentValue.get(fieldName);
                Object newFieldValue = newValue.get(fieldName);
                boolean changed = false;
                if (currentFieldValue != null) {
                    if (!currentFieldValue.equals(newFieldValue)) {
                        changed = true;
                    }
                } else {
                    if (newFieldValue != null) {
                        changed = true;
                    }
                }

                if (changed) {
                    String errMsg = "Watching entity [" + currentValue.getEntityName() + "] field [" + fieldName + "] value changed from ["
                            + currentFieldValue + "] to [" + newFieldValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                    Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
                }
            } else {
                // watch the whole entity
                if (!currentValue.equals(newValue)) {
                    String errMsg = "Watching entity [" + currentValue.getEntityName() + "] values changed from [" + currentValue + "] to ["
                            + newValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                    Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
                }
            }
        } else {
            if (UtilValidate.isNotEmpty(fieldName)) {
                // just watch the field
                Object newFieldValue = newValue.get(fieldName);
                String errMsg = "Watching entity [" + newValue.getEntityName() + "] field [" + fieldName + "] value changed from [null] to ["
                        + newFieldValue + "] for pk [" + newValue.getPrimaryKey() + "]";
                Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
            } else {
                // watch the whole entity
                String errMsg = "Watching entity [" + newValue.getEntityName() + "] values changed from [null] to [" + newValue + "] for pk ["
                        + newValue.getPrimaryKey() + "]";
                Debug.logInfo(new Exception(errMsg), errMsg, MODULE);
            }
        }

        return ServiceUtil.returnSuccess();
    }
}
