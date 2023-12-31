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
package org.sitenetsoft.sunseterp.framework.minilang.method.serviceops;

import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;field-to-result&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class FieldToResult extends MethodOperation {

    private static final String MODULE = FieldToResult.class.getName();

    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleMapAccessor<Object> resultFma;

    public FieldToResult(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "result-name");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field", "result-name");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        String resultNameAttribute = element.getAttribute("result-name");
        if (resultNameAttribute.isEmpty()) {
            this.resultFma = this.fieldFma;
        } else {
            this.resultFma = FlexibleMapAccessor.getInstance(resultNameAttribute);
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        Object fieldVal = this.fieldFma.get(methodContext.getEnvMap());
        if (fieldVal != null) {
            if (this.resultFma.containsNestedExpression()) {
                /*
                 *  Replace FMA nested expression functionality with our own.
                 *  The nested expression must be evaluated once using the
                 *  method context, [methodContext.getEnvMap()] then again to
                 *  place the value in the result Map [methodContext.getResults()].
                 */
                FlexibleStringExpander fse = FlexibleStringExpander.getInstance(this.resultFma.getOriginalName());
                String expression = fse.expandString(methodContext.getEnvMap());
                FlexibleMapAccessor<Object> resultFma = FlexibleMapAccessor.getInstance(expression);
                resultFma.put(methodContext.getResults(), fieldVal);
            } else {
                this.resultFma.put(methodContext.getResults(), fieldVal);
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<field-to-result ");
        if (!this.fieldFma.isEmpty()) {
            sb.append("field=\"").append(this.fieldFma).append("\" ");
        }
        if (!this.resultFma.isEmpty()) {
            sb.append("result-name=\"").append(this.resultFma).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;field-to-request&gt; element.
     */
    public static final class FieldToResultFactory implements Factory<FieldToResult> {
        @Override
        public FieldToResult createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FieldToResult(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "field-to-result";
        }
    }
}
