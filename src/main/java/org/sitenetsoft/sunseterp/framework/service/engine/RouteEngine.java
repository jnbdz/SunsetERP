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
package org.sitenetsoft.sunseterp.framework.service.engine;

import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceDispatcher;

import java.util.Map;

/**
 * RouteEngine.java
 */
public class RouteEngine extends GenericAsyncEngine {

    public RouteEngine(ServiceDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSync(String, ModelService, Map)
     */
    @Override
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        return modelService.makeValid(context, ModelService.OUT_PARAM);
    }

    /**
     * @see org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine#runSyncIgnore(String, ModelService, Map)
     */
    @Override
    public void runSyncIgnore(String localName, ModelService modelService, Map<String, Object> context) throws GenericServiceException {
        return;
    }

    @Override
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, int mode) throws GenericServiceException {
    }

    @Override
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, Map<String, Object> result, int mode)
            throws GenericServiceException {
    }

    @Override
    public void sendCallbacks(ModelService modelService, Map<String, Object> context, Throwable t, int mode) throws GenericServiceException {
    }
}
