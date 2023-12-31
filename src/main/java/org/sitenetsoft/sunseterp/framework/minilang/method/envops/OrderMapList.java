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
package org.sitenetsoft.sunseterp.framework.minilang.method.envops;

import org.sitenetsoft.sunseterp.framework.base.util.UtilXml;
import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.base.util.collections.MapComparator;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangRuntimeException;
import org.sitenetsoft.sunseterp.framework.minilang.MiniLangValidate;
import org.sitenetsoft.sunseterp.framework.minilang.SimpleMethod;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodContext;
import org.sitenetsoft.sunseterp.framework.minilang.method.MethodOperation;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements the &lt;order-map-list&gt; element.
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Referenc</a>
 */
public final class OrderMapList extends MethodOperation {

    private final FlexibleMapAccessor<List<Map<Object, Object>>> listFma;
    private final MapComparator mc;

    public OrderMapList(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.attributeNames(simpleMethod, element, "list");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "list");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "list");
            MiniLangValidate.childElements(simpleMethod, element, "order-by");
            MiniLangValidate.requiredChildElements(simpleMethod, element, "order-by");
        }
        listFma = FlexibleMapAccessor.getInstance(element.getAttribute("list"));
        List<? extends Element> orderByElements = UtilXml.childElementList(element, "order-by");
        if (!orderByElements.isEmpty()) {
            List<FlexibleMapAccessor<String>> orderByList = new ArrayList<>(orderByElements.size());
            for (Element orderByElement : orderByElements) {
                FlexibleMapAccessor<String> fma = FlexibleMapAccessor.getInstance(orderByElement.getAttribute("field"));
                orderByList.add(fma);
            }
            mc = new MapComparator(orderByList);
        } else {
            mc = null;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        if (mc == null) {
            throw new MiniLangRuntimeException("order-by sub-elements not found.", this);
        }
        List<Map<Object, Object>> orderList = listFma.get(methodContext.getEnvMap());
        if (orderList != null) {
            orderList.sort(mc);
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<order-map-list ");
        sb.append("list=\"").append(this.listFma).append("\" />");
        return sb.toString();
    }

    /**
     * A factory for the &lt;order-map-list&gt; element.
     */
    public static final class OrderMapListFactory implements Factory<OrderMapList> {
        @Override
        public OrderMapList createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new OrderMapList(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "order-map-list";
        }
    }
}
