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

import org.sitenetsoft.sunseterp.framework.base.config.GenericConfigException;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.ModelService;
import org.sitenetsoft.sunseterp.framework.service.ServiceDispatcher;
import org.sitenetsoft.sunseterp.framework.service.config.ServiceConfigUtil;
import org.sitenetsoft.sunseterp.framework.service.config.model.ServiceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract Service Engine
 */
public abstract class AbstractEngine implements GenericEngine {

    private static final String MODULE = AbstractEngine.class.getName();
    /** Map containing aliases for service implementation locations. */
    private static final Map<String, String> LOCATION_MAP = createLocationMap();

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
    public ServiceDispatcher getDispatcher() {
        return dispatcher;
    }

    private ServiceDispatcher dispatcher;

    protected AbstractEngine(ServiceDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Instantiates the location map.
     * @return an immutable location map.
     */
    protected static Map<String, String> createLocationMap() {
        Map<String, String> tmp = new HashMap<>();
        List<ServiceLocation> locations;
        try {
            locations = ServiceConfigUtil.getServiceEngine().getServiceLocations();
        } catch (GenericConfigException e) {
            // FIXME: Refactor API so exceptions can be thrown and caught.
            Debug.logError(e, MODULE);
            throw new RuntimeException(e.getMessage());
        }
        locations.forEach(loc -> tmp.put(loc.getName(), loc.getLocation()));
        Debug.logInfo("Loaded Service Locations: " + tmp, MODULE);
        return Collections.unmodifiableMap(tmp);
    }

    /**
     * Looks for location aliases which are set by {@code service-location} elements
     * inside the {@code serviceengine.xml} configuration file.
     * @param model  the object representing a service
     * @return the actual location where to find the service implementation
     */
    protected String getLocation(ModelService model) {
        return LOCATION_MAP.getOrDefault(model.getLocation(), model.getLocation());
    }

    @Override
    public void sendCallbacks(ModelService model, Map<String, Object> context, int mode)
            throws GenericServiceException {
        if (allowCallbacks(model, context, mode)) {
            dispatcher.getCallbacks(model.getName()).forEach(gsc -> gsc.receiveEvent(context));
        }
    }

    @Override
    public void sendCallbacks(ModelService model, Map<String, Object> context, Throwable t, int mode)
            throws GenericServiceException {
        if (allowCallbacks(model, context, mode)) {
            dispatcher.getCallbacks(model.getName()).forEach(gsc -> gsc.receiveEvent(context, t));
        }
    }

    @Override
    public void sendCallbacks(ModelService model, Map<String, Object> context, Map<String, Object> result, int mode)
            throws GenericServiceException {
        if (allowCallbacks(model, context, mode)) {
            dispatcher.getCallbacks(model.getName()).forEach(gsc -> gsc.receiveEvent(context, result));
        }
    }

    /**
     * Allow callbacks boolean.
     * @param model the model
     * @param context the context
     * @param mode the mode
     * @return the boolean
     * @throws GenericServiceException the generic service exception
     */
    protected boolean allowCallbacks(ModelService model, Map<String, Object> context, int mode) throws GenericServiceException {
        return true;
    }
}
