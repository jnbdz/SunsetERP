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
package org.sitenetsoft.sunseterp.framework.minilang.operation;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilURL;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.w3c.dom.Element;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A MakeInStringOperation that insert the value of a property from a properties file
 */
public class PropertyOper extends MakeInStringOperation {

    private static final String MODULE = PropertyOper.class.getName();

    private String property;
    private String resource;

    public PropertyOper(Element element) {
        super(element);
        resource = element.getAttribute("resource");
        property = element.getAttribute("property");
    }

    @Override
    public String exec(Map<String, Object> inMap, List<Object> messages, Locale locale, ClassLoader loader) {
        URL url = UtilURL.fromResource(resource, loader);
        String propStr = UtilProperties.getPropertyValue(url, property);

        if (UtilValidate.isEmpty(propStr)) {
            Debug.logWarning("[SimpleMapProcessor.PropertyOper.exec] Property " + property + " in resource " + resource
                    + " not found, not appending anything", MODULE);
            return null;
        } else {
            return propStr;
        }
    }
}
