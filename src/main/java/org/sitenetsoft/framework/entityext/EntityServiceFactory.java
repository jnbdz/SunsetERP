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

import org.sitenetsoft.framework.entity.Delegator;
import org.sitenetsoft.framework.service.DispatchContext;
import org.sitenetsoft.framework.service.LocalDispatcher;
import org.sitenetsoft.framework.service.ServiceContainer;

/**
 * EntityEcaUtil
 */
public class EntityServiceFactory {

    private static final String MODULE = EntityServiceFactory.class.getName();

    public static LocalDispatcher getLocalDispatcher(Delegator delegator) {
        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher("entity-" + delegator.getDelegatorName(), delegator);
        return dispatcher;
    }

    public static DispatchContext getDispatchContext(Delegator delegator) {
        LocalDispatcher dispatcher = getLocalDispatcher(delegator);
        if (dispatcher == null) return null;
        return dispatcher.getDispatchContext();
    }
}
