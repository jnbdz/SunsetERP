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
package org.sitenetsoft.sunseterp.framework.entity.finder;

import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public abstract class Finder implements Serializable {
    private FlexibleStringExpander entityNameExdr;

    /**
     * Gets entity name exdr.
     * @return the entity name exdr
     */
    public FlexibleStringExpander getEntityNameExdr() {
        return entityNameExdr;
    }

    private FlexibleStringExpander useCacheStrExdr;

    /**
     * Gets use cache str exdr.
     * @return the use cache str exdr
     */
    public FlexibleStringExpander getUseCacheStrExdr() {
        return useCacheStrExdr;
    }

    protected Finder(Element element) {
        this.entityNameExdr = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        this.useCacheStrExdr = FlexibleStringExpander.getInstance(element.getAttribute("use-cache"));
    }

    /**
     * Gets entity name.
     * @return the entity name
     */
    public String getEntityName() {
        String entName = this.entityNameExdr.getOriginal();
        // if there is expansion syntax
        if (entName.indexOf("${") >= 0) {
            return null;
        }
        return entName;
    }

    /**
     * Sets entity name.
     * @param entityName the entity name
     */
    public void setEntityName(String entityName) {
        this.entityNameExdr = FlexibleStringExpander.getInstance(entityName);
    }

    public abstract void runFind(Map<String, Object> context, Delegator delegator) throws GeneralException;
}

