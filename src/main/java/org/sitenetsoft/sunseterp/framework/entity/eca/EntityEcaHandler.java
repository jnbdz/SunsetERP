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
package org.sitenetsoft.sunseterp.framework.entity.eca;

import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntity;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;

import java.util.List;
import java.util.Map;

/**
 * EntityEcaHandler interface
 *
 */
public interface EntityEcaHandler<T> {

    String EV_VALIDATE = "validate";
    String EV_RUN = "run";
    String EV_RETURN = "return";
    /**
     * Invoked after the entity operation, but before the cache is cleared.
     */
    String EV_CACHE_CLEAR = "cache-clear";
    String EV_CACHE_CHECK = "cache-check";
    String EV_CACHE_PUT = "cache-put";

    String OP_CREATE = "create";
    String OP_STORE = "store";
    String OP_REMOVE = "remove";
    String OP_FIND = "find";


    void setDelegator(Delegator delegator);

    Map<String, List<T>> getEntityEventMap(String entityName);

    void evalRules(String currentOperation, Map<String, List<T>> eventMap, String event, GenericEntity value, boolean isError)
            throws GenericEntityException;
}
