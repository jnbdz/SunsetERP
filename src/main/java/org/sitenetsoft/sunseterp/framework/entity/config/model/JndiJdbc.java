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
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;jndi-jdbc&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class JndiJdbc extends JdbcElement {

    private final String jndiServerName; // type = xs:string
    private final String jndiName; // type = xs:string

    JndiJdbc(Element element) throws GenericEntityConfException {
        super(element);
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String jndiServerName = element.getAttribute("jndi-server-name").intern();
        if (jndiServerName.isEmpty()) {
            throw new GenericEntityConfException("<jndi-jdbc> element jndi-server-name attribute is empty" + lineNumberText);
        }
        this.jndiServerName = jndiServerName;
        String jndiName = element.getAttribute("jndi-name").intern();
        if (jndiName.isEmpty()) {
            throw new GenericEntityConfException("<jndi-jdbc> element jndi-name attribute is empty" + lineNumberText);
        }
        this.jndiName = jndiName;
    }

    /** Returns the value of the <code>jndi-server-name</code> attribute. */
    public String getJndiServerName() {
        return this.jndiServerName;
    }

    /** Returns the value of the <code>jndi-name</code> attribute. */
    public String getJndiName() {
        return this.jndiName;
    }
}
