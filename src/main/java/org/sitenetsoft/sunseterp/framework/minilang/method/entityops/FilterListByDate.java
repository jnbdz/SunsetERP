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

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime;
import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntity;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

import java.sql.Timestamp;
import java.util.List;

/**
 * Implements the &lt;filter-list-by-date&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class FilterListByDate extends MethodOperation {

    private final FlexibleMapAccessor<List<GenericEntity>> listFma;
    private final FlexibleMapAccessor<List<GenericEntity>> toListFma;
    private final FlexibleMapAccessor<Timestamp> validDateFma;
    private final String fromFieldName;
    private final String thruFieldName;

    public FilterListByDate(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list", "to-list", "valid-date", "fromDate", "thruDate");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list", "to-list", "valid-date");
            MiniLangValidate.constantAttributes(simpleMethod, element, "fromDate", "thruDate");
            MiniLangValidate.noChildElements(simpleMethod, element);
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        String toListAttribute = element.getAttribute("to-list");
        if (toListAttribute.isEmpty()) {
            toListFma = listFma;
        } else {
            toListFma = FlexibleMapAccessor.getInstance(toListAttribute);
        }
        validDateFma = FlexibleMapAccessor.getInstance(element.getAttribute("valid-date"));
        fromFieldName = MiniLangValidate.checkAttribute(element.getAttribute("from-field-name"), "fromDate");
        thruFieldName = MiniLangValidate.checkAttribute(element.getAttribute("thru-field-name"), "thruDate");
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (!validDateFma.isEmpty()) {
            toListFma.put(methodContext.getEnvMap(), EntityUtil.filterByDate(listFma.get(methodContext.getEnvMap()),
                    validDateFma.get(methodContext.getEnvMap()), fromFieldName, thruFieldName, true));
        } else {
            toListFma.put(methodContext.getEnvMap(), EntityUtil.filterByDate(listFma.get(methodContext.getEnvMap()),
                    UtilDateTime.nowTimestamp(), fromFieldName, thruFieldName, true));
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<filter-list-by-date ");
        sb.append("list=\"").append(this.listFma).append("\" ");
        sb.append("to-list=\"").append(this.toListFma).append("\" ");
        sb.append("valid-date=\"").append(this.validDateFma).append("\" ");
        sb.append("from-field-name=\"").append(this.fromFieldName).append("\" ");
        sb.append("thru-field-name=\"").append(this.thruFieldName).append("\" ");
        sb.append("/>");
        return sb.toString();
    }

    /**
     * A factory for the &lt;filter-list-by-date&gt; element.
     */
    public static final class FilterListByDateFactory implements Factory<FilterListByDate> {
        @Override
        public FilterListByDate createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new FilterListByDate(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "filter-list-by-date";
        }
    }
}
