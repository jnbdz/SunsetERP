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
package org.sitenetsoft.framework.service.config.model;

import org.sitenetsoft.framework.base.lang.ThreadSafe;
import org.sitenetsoft.framework.service.config.ServiceConfigException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;service-ecas&gt;</code> element.
 */
@ThreadSafe
public final class ServiceEcas {

    private final String loader;
    private final String location;

    ServiceEcas(Element serviceEcasElement) throws ServiceConfigException {
        String loader = serviceEcasElement.getAttribute("loader").intern();
        if (loader.isEmpty()) {
            throw new ServiceConfigException("<service-ecas> element loader attribute is empty");
        }
        this.loader = loader;
        String location = serviceEcasElement.getAttribute("location").intern();
        if (location.isEmpty()) {
            throw new ServiceConfigException("<service-ecas> element location attribute is empty");
        }
        this.location = location;
    }

    public String getLoader() {
        return loader;
    }

    public String getLocation() {
        return location;
    }
}