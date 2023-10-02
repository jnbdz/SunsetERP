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
package org.sitenetsoft.framework.service.group;

import org.sitenetsoft.framework.service.GenericServiceException;
import org.sitenetsoft.framework.service.ModelService;
import org.sitenetsoft.framework.service.ServiceDispatcher;
import org.sitenetsoft.framework.service.engine.GenericAsyncEngine;

import java.util.Map;

/**
 * ServiceGroupEngine.java
 */
public class ServiceGroupEngine extends GenericAsyncEngine {

    /**
     * Constructor for ServiceGroupEngine.
     * @param dispatcher
     */
    public ServiceGroupEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.framework.service.engine.GenericEngine#runSync(String, ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        GroupModel groupModel = modelService.getInternalGroup();
        if (groupModel == null) {
            groupModel = ServiceGroupReader.getGroupModel(this.getLocation(modelService));
        }
        if (groupModel == null) {
            throw new GenericServiceException("GroupModel was null; not a valid ServiceGroup!");
        }

        return groupModel.run(getDispatcher(), localName, context);
    }

    /**
     * @see org.sitenetsoft.framework.service.engine.GenericEngine#runSyncIgnore(String, ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        runSync(localName, modelService, context);
    }
}
