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

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangRuntimeException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;remove-value&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class RemoveValue extends MethodOperation {

    private static final String MODULE = RemoveValue.class.getName();
    private final FlexibleMapAccessor<GenericValue> valueFma;

    public RemoveValue(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "value-field", "do-cache-clear");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "value-field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "value-field");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        valueFma = FlexibleMapAccessor.getInstance(element.getAttribute("value-field"));
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        GenericValue value = valueFma.get(methodContext.getEnvMap());
        if (value == null) {
            throw new MiniLangRuntimeException("Entity value not found with name: " + valueFma, this);
        }
        try {
            value.getDelegator().removeValue(value);
        } catch (GenericEntityException e) {
            String errMsg = "Exception thrown while removing entity value: " + e.getMessage();
            Debug.logWarning(e, errMsg, MODULE);
            getSimpleMethod().addErrorMessage(methodContext, errMsg);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<remove-value ");
        sb.append("value-field=\"").append(this.valueFma).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;remove-value&gt; element.
     */
    public static final class RemoveValueFactory implements Factory<RemoveValue> {
        @Override
        public RemoveValue createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new RemoveValue(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "remove-value";
        }
    }
}
