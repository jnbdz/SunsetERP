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
package org.sitenetsoft.sunseterp.framework.minilang.method.entityops;

import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangRuntimeException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.artifact.ArtifactInfoContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Implements the &lt;make-value&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class MakeValue extends EntityOperation {

    private final FlexibleStringExpander entityNameFse;
    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public MakeValue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "entity-name", "map", "delegator-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "entity-name");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "map", "delegator-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        entityNameFse = FlexibleStringExpander.getInstance(element.getAttribute("entity-name"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String entityName = entityNameFse.expandString(methodContext.getEnvMap());
        if (entityName.isEmpty()) {
            throw new MiniLangRuntimeException("Entity name not found.", this);
        }
        Delegator delegator = getDelegator(methodContext);
        valueFma.put(methodContext.getEnvMap(), delegator.makeValidValue(entityName, mapFma.get(methodContext.getEnvMap())));
        return true;
    }

    @Override
    public void gatherArtifactInfo(ArtifactInfoContext aic) {
        aic.addEntityName(entityNameFse.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<make-value ");
        sb.append("entity-name=\"").append(this.entityNameFse).append("\" ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        if (!mapFma.isEmpty()) {
            sb.append("map=\"").append(this.mapFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;make-value&gt; element.
     */
    public static final class MakeValueFactory implements Factory<MakeValue> {
        @Override
        public MakeValue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new MakeValue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "make-value";
        }
    }
}
