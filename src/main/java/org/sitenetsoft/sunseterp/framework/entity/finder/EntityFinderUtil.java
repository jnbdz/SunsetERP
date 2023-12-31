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

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.collections.FlexibleMapAccessor;
import org.sitenetsoft.sunseterp.framework.base.util.string.FlexibleStringExpander;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.*;
//import org.sitenetsoft.sunseterp.framework.entity.condition.
//import org.sitenetsoft.sunseterp.framework.minilang.method.entityops.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelFieldTypeReader;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityListIterator;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.*;

import static org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics.cast;

/**
 * Uses the delegator to find entity values by a condition
 *
 */
public final class EntityFinderUtil {

    private static final String MODULE = EntityFinderUtil.class.getName();

    private EntityFinderUtil() { }

    public static Map<FlexibleMapAccessor<Object>, Object> makeFieldMap(Element element) {
        Map<FlexibleMapAccessor<Object>, Object> fieldMap = null;
        List<? extends Element> fieldMapElementList = UtilXml.childElementList(element, "field-map");
        if (!fieldMapElementList.isEmpty()) {
            fieldMap = new HashMap<>(fieldMapElementList.size());
            for (Element fieldMapElement: fieldMapElementList) {
                // set the env-name for each field-name, noting that if no field-name is specified it defaults to the env-name
                String fieldName = fieldMapElement.getAttribute("field-name");
                String envName = fieldMapElement.getAttribute("from-field");
                if (envName.isEmpty()) {
                    envName = fieldMapElement.getAttribute("env-name");
                }
                String value = fieldMapElement.getAttribute("value");
                if (fieldName.isEmpty()) {
                    // no fieldName, use envName for both
                    fieldMap.put(FlexibleMapAccessor.getInstance(envName), FlexibleMapAccessor.getInstance(envName));
                } else {
                    if (!value.isEmpty()) {
                        fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleStringExpander.getInstance(value));
                    } else {
                        // at this point we have a fieldName and no value, do we have a envName?
                        if (!envName.isEmpty()) {
                            fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleMapAccessor.getInstance(envName));
                        } else {
                            // no envName, use fieldName for both
                            fieldMap.put(FlexibleMapAccessor.getInstance(fieldName), FlexibleMapAccessor.getInstance(fieldName));
                        }
                    }
                }
            }
        }
        return fieldMap;
    }

    public static void expandFieldMapToContext(Map<FlexibleMapAccessor<Object>, Object> fieldMap, Map<String, Object> context, Map<String, Object>
            outContext) {
        if (fieldMap != null) {
            for (Map.Entry<FlexibleMapAccessor<Object>, Object> entry: fieldMap.entrySet()) {
                FlexibleMapAccessor<Object> serviceContextFieldAcsr = entry.getKey();
                Object valueSrc = entry.getValue();
                if (valueSrc instanceof FlexibleMapAccessor<?>) {
                    FlexibleMapAccessor<Object> contextEnvAcsr = cast(valueSrc);
                    serviceContextFieldAcsr.put(outContext, contextEnvAcsr.get(context));
                } else if (valueSrc instanceof FlexibleStringExpander) {
                    FlexibleStringExpander valueExdr = (FlexibleStringExpander) valueSrc;
                    serviceContextFieldAcsr.put(outContext, valueExdr.expandString(context));
                //} else {
                    // hmmmm...
                }
            }
        }
    }

    public static List<FlexibleStringExpander> makeSelectFieldExpanderList(Element element) {
        List<FlexibleStringExpander> selectFieldExpanderList = null;
        List<? extends Element> selectFieldElementList = UtilXml.childElementList(element, "select-field");
        if (!selectFieldElementList.isEmpty()) {
            selectFieldExpanderList = new ArrayList<>(selectFieldElementList.size());
            for (Element selectFieldElement: selectFieldElementList) {
                selectFieldExpanderList.add(FlexibleStringExpander.getInstance(selectFieldElement.getAttribute("field-name")));
            }
        }
        return selectFieldExpanderList;
    }

    public static Set<String> makeFieldsToSelect(List<FlexibleStringExpander> selectFieldExpanderList, Map<String, Object> context) {
        Set<String> fieldsToSelect = null;
        if (UtilValidate.isNotEmpty(selectFieldExpanderList)) {
            fieldsToSelect = new HashSet<>();
            for (FlexibleStringExpander selectFieldExpander: selectFieldExpanderList) {
                fieldsToSelect.add(selectFieldExpander.expandString(context));
            }
        }
        return fieldsToSelect;
    }

    public static List<String> makeOrderByFieldList(List<FlexibleStringExpander> orderByExpanderList, Map<String, Object> context) {
        List<String> orderByFields = null;
        if (UtilValidate.isNotEmpty(orderByExpanderList)) {
            orderByFields = new ArrayList<>(orderByExpanderList.size());
            for (FlexibleStringExpander orderByExpander: orderByExpanderList) {
                orderByFields.add(orderByExpander.expandString(context));
            }
        }
        return orderByFields;
    }

    public interface Condition extends Serializable {
        EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader modelFieldTypeReader);
    }

    @SuppressWarnings("serial")
    public static final class ConditionExpr implements Condition {
        private final String fieldName;
        private final EntityOperator<?, ?> operator;
        private final FlexibleMapAccessor<Object> envNameAcsr;
        private final FlexibleStringExpander valueExdr;
        private final FlexibleStringExpander ignoreExdr;
        private final boolean ignoreIfNull;
        private final boolean ignoreIfEmpty;
        private final boolean ignoreCase;

        public ConditionExpr(Element conditionExprElement) {
            String fieldNameAttribute = conditionExprElement.getAttribute("field-name");
            if (fieldNameAttribute.isEmpty()) {
                fieldNameAttribute = conditionExprElement.getAttribute("name");
            }
            this.fieldName = fieldNameAttribute;
            String operatorAttribute = conditionExprElement.getAttribute("operator");
            if (operatorAttribute.isEmpty()) {
                operatorAttribute = "equals";
            }
            this.operator = EntityOperator.lookup(operatorAttribute);
            if (this.operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorAttribute);
            }
            String fromFieldAttribute = conditionExprElement.getAttribute("from-field");
            if (fromFieldAttribute.isEmpty()) {
                fromFieldAttribute = conditionExprElement.getAttribute("env-name");
            }
            this.envNameAcsr = FlexibleMapAccessor.getInstance(fromFieldAttribute);
            this.valueExdr = FlexibleStringExpander.getInstance(conditionExprElement.getAttribute("value"));
            this.ignoreIfNull = "true".equals(conditionExprElement.getAttribute("ignore-if-null"));
            this.ignoreIfEmpty = "true".equals(conditionExprElement.getAttribute("ignore-if-empty"));
            this.ignoreCase = "true".equals(conditionExprElement.getAttribute("ignore-case"));
            this.ignoreExdr = FlexibleStringExpander.getInstance(conditionExprElement.getAttribute("ignore"));
        }

        @Override
        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader
                modelFieldTypeReader) {
            if ("true".equals(this.ignoreExdr.expandString(context))) {
                return null;
            }
            if (modelEntity.getField(fieldName) == null) {
                throw new IllegalArgumentException("Error in Entity Find: could not find field [" + fieldName + "] in entity with name ["
                        + modelEntity.getEntityName() + "]");
            }

            Object value = envNameAcsr.get(context);
            if (value == null && !valueExdr.isEmpty()) {
                value = valueExdr.expandString(context);
            }
            if (this.ignoreIfNull && value == null) {
                return null;
            }

            // If IN or BETWEEN operator, see if value is a literal list and split it
            if ((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN) || operator.equals(EntityOperator.NOT_IN))
                    && value instanceof String) {
                String delim = null;
                if (((String) value).indexOf('|') >= 0) {
                    delim = "|";
                } else if (((String) value).indexOf(',') >= 0) {
                    delim = ",";
                }
                if (delim != null) {
                    value = StringUtil.split((String) value, delim);
                }
            }

            // don't convert the field to the desired type if this is an IN or BETWEEN operator and we have a Collection
            if (!((operator.equals(EntityOperator.IN) || operator.equals(EntityOperator.BETWEEN) || operator.equals(EntityOperator.NOT_IN))
                    && value instanceof Collection<?>)) {
                // now to a type conversion for the target fieldName
                value = modelEntity.convertFieldValue(modelEntity.getField(fieldName), value, modelFieldTypeReader, context);
            }

            if (Debug.verboseOn()) {
                Debug.logVerbose("Got value for fieldName [" + fieldName + "]: " + value, MODULE);
            }

            if (this.ignoreIfEmpty && ObjectType.isEmpty(value)) {
                return null;
            }

            if (operator == EntityOperator.NOT_EQUAL && value != null) {
                // since some databases don't consider nulls in != comparisons, explicitly include them
                // this makes more sense logically, but if anyone ever needs it to not behave this way we should add an "or-null" attribute
                // that is true by default
                if (ignoreCase) {
                    return EntityCondition.makeCondition(
                            EntityCondition.makeCondition(EntityFunction.upperField(fieldName),
                                    UtilGenerics.<EntityComparisonOperator<?, ?>>cast(operator), EntityFunction.upper(value)),
                            EntityOperator.OR,
                            EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null));
                }
                return EntityCondition.makeCondition(
                        EntityCondition.makeCondition(fieldName, UtilGenerics.<EntityComparisonOperator<?, ?>>cast(operator), value),
                        EntityOperator.OR,
                        EntityCondition.makeCondition(fieldName, EntityOperator.EQUALS, null));
            }
            if (ignoreCase) {
                // use the stuff to upper case both sides
                return EntityCondition.makeCondition(EntityFunction.upperField(fieldName),
                        UtilGenerics.<EntityComparisonOperator<?, ?>>cast(operator), EntityFunction.upper(value));
            }
            return EntityCondition.makeCondition(fieldName, UtilGenerics.<EntityComparisonOperator<?, ?>>cast(operator), value);
        }
    }

    @SuppressWarnings("serial")
    public static final class ConditionList implements Condition {
        private final List<Condition> conditionList;
        private final EntityOperator<?, ?> operator;

        public ConditionList(Element conditionListElement) {
            String operatorAttribute = conditionListElement.getAttribute("combine");
            if (operatorAttribute.isEmpty()) {
                operatorAttribute = "and";
            }
            this.operator = EntityOperator.lookup(operatorAttribute);
            if (this.operator == null) {
                throw new IllegalArgumentException("Could not find an entity operator for the name: " + operatorAttribute);
            }
            List<? extends Element> subElements = UtilXml.childElementList(conditionListElement);
            if (subElements.isEmpty()) {
                this.conditionList = null;
            } else {
                List<Condition> conditionList = new ArrayList<>(subElements.size());
                for (Element subElement : subElements) {
                    switch (subElement.getLocalName()) {
                    case "condition-expr":
                        conditionList.add(new ConditionExpr(subElement));
                        break;
                    case "condition-list":
                        conditionList.add(new ConditionList(subElement));
                        break;
                    case "condition-object":
                        conditionList.add(new ConditionObject(subElement));
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid element with name [" + subElement.getNodeName()
                                + "] found under a condition-list element.");
                    }
                }
                this.conditionList = Collections.unmodifiableList(conditionList);
            }
        }

        @Override
        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader
                modelFieldTypeReader) {
            if (this.conditionList == null) {
                return null;
            }
            if (this.conditionList.size() == 1) {
                Condition condition = this.conditionList.get(0);
                return condition.createCondition(context, modelEntity, modelFieldTypeReader);
            }
            List<EntityCondition> entityConditionList = new ArrayList<>(this.conditionList.size());
            for (Condition curCondition: this.conditionList) {
                EntityCondition econd = curCondition.createCondition(context, modelEntity, modelFieldTypeReader);
                if (econd != null) {
                    entityConditionList.add(econd);
                }
            }
            return EntityCondition.makeCondition(entityConditionList, UtilGenerics.<EntityJoinOperator>cast(operator));
        }
    }

    @SuppressWarnings("serial")
    public static final class ConditionObject implements Condition {
        private final FlexibleMapAccessor<Object> fieldNameAcsr;

        public ConditionObject(Element conditionExprElement) {
            String fieldNameAttribute = conditionExprElement.getAttribute("field");
            if (fieldNameAttribute.isEmpty()) {
                fieldNameAttribute = conditionExprElement.getAttribute("field-name");
            }
            this.fieldNameAcsr = FlexibleMapAccessor.getInstance(fieldNameAttribute);
        }

        @Override
        public EntityCondition createCondition(Map<String, ? extends Object> context, ModelEntity modelEntity, ModelFieldTypeReader
                modelFieldTypeReader) {
            return (EntityCondition) fieldNameAcsr.get(context);
        }
    }

    public interface OutputHandler extends Serializable {
        void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr);
        void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr);
    }

    @SuppressWarnings("serial")
    public static class LimitRange implements OutputHandler {
        private FlexibleStringExpander startExdr;
        private FlexibleStringExpander sizeExdr;

        public LimitRange(Element limitRangeElement) {
            this.startExdr = FlexibleStringExpander.getInstance(limitRangeElement.getAttribute("start"));
            this.sizeExdr = FlexibleStringExpander.getInstance(limitRangeElement.getAttribute("size"));
        }

        /**
         * Gets start.
         * @param context the context
         * @return the start
         */
        int getStart(Map<String, Object> context) {
            String startStr = this.startExdr.expandString(context);
            try {
                return Integer.parseInt(startStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range start number \"" + startStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        /**
         * Gets size.
         * @param context the context
         * @return the size
         */
        int getSize(Map<String, Object> context) {
            String sizeStr = this.sizeExdr.expandString(context);
            try {
                return Integer.parseInt(sizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-range size number \"" + sizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        @Override
        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            int start = getStart(context) + 1; // ELI index is one-based.
            int size = getSize(context);
            try {
                listAcsr.put(context, eli.getPartialList(start, size));
                eli.close();
            } catch (GenericEntityException e) {
                String errMsg = "Error getting partial list in limit-range with start=" + start + " and size=" + size + ": " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        @Override
        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            List<GenericValue> result = null;
            int start = getStart(context);
            if (start < results.size()) {
                int size = getSize(context);
                int end = start + size;
                if (end > results.size()) {
                    end = results.size();
                }
                result = results.subList(start, end);
            } else {
                result = new LinkedList<>();
            }
            listAcsr.put(context, result);
        }
    }

    @SuppressWarnings("serial")
    public static class LimitView implements OutputHandler {
        private FlexibleStringExpander viewIndexExdr;
        private FlexibleStringExpander viewSizeExdr;

        public LimitView(Element limitViewElement) {
            this.viewIndexExdr = FlexibleStringExpander.getInstance(limitViewElement.getAttribute("view-index"));
            this.viewSizeExdr = FlexibleStringExpander.getInstance(limitViewElement.getAttribute("view-size"));
        }

        /**
         * Gets index.
         * @param context the context
         * @return the index
         */
        int getIndex(Map<String, Object> context) {
            String viewIndexStr = this.viewIndexExdr.expandString(context);
            try {
                return Integer.parseInt(viewIndexStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-index number \"" + viewIndexStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        /**
         * Gets size.
         * @param context the context
         * @return the size
         */
        int getSize(Map<String, Object> context) {
            String viewSizeStr = this.viewSizeExdr.expandString(context);
            try {
                return Integer.parseInt(viewSizeStr);
            } catch (NumberFormatException e) {
                String errMsg = "The limit-view view-size number \"" + viewSizeStr + "\" was not valid: " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        @Override
        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            int index = this.getIndex(context);
            int size = this.getSize(context);
            try {
                listAcsr.put(context, eli.getPartialList(((index - 1) * size) + 1, size)); // ELI index is one-based.
                eli.close();
            } catch (GenericEntityException e) {
                String errMsg = "Error getting partial list in limit-view with index=" + index + " and size=" + size + ": " + e.toString();
                Debug.logError(e, errMsg, MODULE);
                throw new IllegalArgumentException(errMsg);
            }
        }

        @Override
        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            List<GenericValue> result = null;
            int index = this.getIndex(context);
            int size = this.getSize(context);
            int begin = (index - 1) * size;
            if (begin < results.size()) {
                int end = begin + size;
                if (end > results.size()) {
                    end = results.size();
                }
                result = results.subList(begin, end);
            } else {
                result = new LinkedList<>();
            }
            listAcsr.put(context, result);
        }
    }

    @SuppressWarnings("serial")
    public static class UseIterator implements OutputHandler {
        public UseIterator(Element useIteratorElement) {
            // no parameters, nothing to do
        }

        @Override
        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            listAcsr.put(context, eli);
        }

        @Override
        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            throw new IllegalArgumentException("Cannot handle output with use-iterator when the query is cached, "
                    + "or the result in general is not an EntityListIterator");
        }
    }
    @SuppressWarnings("serial")
    public static class GetAll implements OutputHandler {
        public GetAll() {
            // no parameters, nothing to do
        }

        @Override
        public void handleOutput(EntityListIterator eli, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            try {
                listAcsr.put(context, eli.getCompleteList());
                eli.close();
            } catch (GenericEntityException e) {
                String errorMsg = "Error getting list from EntityListIterator: " + e.toString();
                Debug.logError(e, errorMsg, MODULE);
                throw new IllegalArgumentException(errorMsg);
            }
        }

        @Override
        public void handleOutput(List<GenericValue> results, Map<String, Object> context, FlexibleMapAccessor<Object> listAcsr) {
            listAcsr.put(context, results);
        }
    }
}

