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
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangRuntimeException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Implements the &lt;set-pk-fields&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class SetPkFields extends MethodOperation {

    private final FlexibleMapAccessor<Map<String, ? extends Object>> mapFma;
    private final FlexibleStringExpander setIfNullFse;
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public SetPkFields(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "set-if-null", "map");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field", "map");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field", "map");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
        setIfNullFse = FlexibleStringExpander.getInstance(element.getAttribute("set-if-null"));
        mapFma = FlexibleMapAccessor.getInstance(element.getAttribute("map"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value == null) {
            throw new MiniLangRuntimeException("Entity value not found with name: " + valueFma, this);
        }
        Map<String, ? extends Object> theMap = mapFma.get(methodContext.getEnvMap());
        if (theMap == null) {
            throw new MiniLangRuntimeException("Map not found with name: " + mapFma, this);
        }
        boolean setIfNull = !"false".equals(setIfNullFse.expand(methodContext.getEnvMap()));
        value.setPKFields(theMap, setIfNull);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<set-pk-fields ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("map=\"").append(this.mapFma).append("\" ");
        if (!setIfNullFse.isEmpty()) {
            sb.append("set-if-null=\"").append(this.setIfNullFse).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;set-pk-fields&gt; element.
     */
    public static final class SetPkFieldsFactory implements Factory<SetPkFields> {
        @Override
        public SetPkFields createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new SetPkFields(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "set-pk-fields";
        }
    }
}
