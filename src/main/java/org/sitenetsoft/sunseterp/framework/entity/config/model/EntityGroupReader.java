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
package org.sitenetsoft.sunseterp.framework.entity.config.model;

import org.sitenetsoft.sunseterp.framework.base.lang.ThreadSafe;
import org.sitenetsoft.sunseterp.framework.base.util.UtilXml;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityConfException;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An object that models the <code>&lt;entity-group-reader&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class EntityGroupReader {

    private final String name; // type = xs:string
    private final String loader; // type = xs:string
    private final String location; // type = xs:string
    private final List<Resource> resourceList; // <resource>

    EntityGroupReader(Element element) throws GenericEntityConfException {
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String name = element.getAttribute("name").intern();
        if (name.isEmpty()) {
            throw new GenericEntityConfException("<entity-group-reader> element name attribute is empty" + lineNumberText);
        }
        this.name = name;
        this.loader = element.getAttribute("loader").intern();
        this.location = element.getAttribute("location").intern();
        List<? extends Element> resourceElementList = UtilXml.childElementList(element, "resource");
        if (resourceElementList.isEmpty()) {
            this.resourceList = Collections.emptyList();
        } else {
            List<Resource> resourceList = new ArrayList<>(resourceElementList.size());
            for (Element resourceElement : resourceElementList) {
                resourceList.add(new Resource(resourceElement));
            }
            this.resourceList = Collections.unmodifiableList(resourceList);
        }
    }

    /** Returns the value of the <code>name</code> attribute. */
    public String getName() {
        return this.name;
    }

    /** Returns the value of the <code>loader</code> attribute. */
    public String getLoader() {
        return this.loader;
    }

    /** Returns the value of the <code>location</code> attribute. */
    public String getLocation() {
        return this.location;
    }

    /** Returns the <code>&lt;resource&gt;</code> child elements. */
    public List<Resource> getResourceList() {
        return this.resourceList;
    }
}
