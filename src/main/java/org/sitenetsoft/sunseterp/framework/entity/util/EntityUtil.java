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

package org.sitenetsoft.sunseterp.framework.entity.util;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.collections.PagedList;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntity;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityDateFilterCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.OrderByList;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelField;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Helper methods when dealing with Entities, especially ones that follow certain conventions
 */
public final class EntityUtil {

    private static final String MODULE = EntityUtil.class.getName();

    private EntityUtil() { }

    @SafeVarargs
    public static <V> Map<String, V> makeFields(V... args) {
        Map<String, V> fields = new HashMap<>();
        if (args != null) {
            for (int i = 0; i < args.length;) {
                V keyValue = args[i];
                if (!(keyValue instanceof String)) throw new IllegalArgumentException("Key(" + i + "), with value(" + args[i] + ") is not a String.");
                String key = (String) keyValue;
                i++;
                V value = args[i];
                if (value != null) {
                    if (!(value instanceof Comparable<?>)) {
                        throw new IllegalArgumentException("Value(" + i + "), with value(" + args[i] + ") does not implement Comparable.");
                    }
                    if (!(value instanceof Serializable)) {
                        throw new IllegalArgumentException("Value(" + i + "), with value(" + args[i] + ") does not implement Serializable.");
                    }
                }
                fields.put(key, value);
                i++;
            }
        }
        return fields;
    }


    public static GenericValue getFirst(Collection<GenericValue> values) {
        if (UtilValidate.isNotEmpty(values)) {
            return values.iterator().next();
        } else {
            return null;
        }
    }

    public static GenericValue getFirst(List<GenericValue> values) {
        if (UtilValidate.isNotEmpty(values)) {
            return values.get(0);
        } else {
            return null;
        }
    }

    public static GenericValue getOnly(Collection<GenericValue> values) {
        if (UtilValidate.isNotEmpty(values)) {
            Iterator<GenericValue> it = values.iterator();
            GenericValue result = it.next();
            if (it.hasNext()) {
                throw new IllegalArgumentException("Passed List had more than one value.");
            }
            return result;
        } else {
            return null;
        }
    }

    public static GenericValue getOnly(List<GenericValue> values) {
        if (UtilValidate.isNotEmpty(values)) {
            if (values.size() == 1) {
                return values.get(0);
            } else {
                throw new IllegalArgumentException("Passed List had more than one value.");
            }
        } else {
            return null;
        }
    }

    public static EntityCondition getFilterByDateExpr() {
        return EntityCondition.makeConditionDate("fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(String fromDateName, String thruDateName) {
        return EntityCondition.makeConditionDate(fromDateName, thruDateName);
    }

    public static EntityCondition getFilterByDateExpr(java.util.Date moment) {
        return EntityDateFilterCondition.makeCondition(new Timestamp(moment.getTime()), "fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(Timestamp moment) {
        return EntityDateFilterCondition.makeCondition(moment, "fromDate", "thruDate");
    }

    public static EntityCondition getFilterByDateExpr(Timestamp moment, String fromDateName, String thruDateName) {
        return EntityDateFilterCondition.makeCondition(moment, fromDateName, thruDateName);
    }

    /**
     * returns the values that are currently active.
     * @param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     * @return List of GenericValue's that are currently active
     */
    public static <T extends GenericEntity> List<T> filterByDate(List<T> datedValues) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), null, null, true);
    }

    /**
     * returns the values that are currently active.
     * @param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     * @param allAreSame  Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we
     *                    only have to see if the from and thru date fields are valid once
     * @return List of GenericValue's that are currently active
     */
    public static <T extends GenericEntity> List<T> filterByDate(List<T> datedValues, boolean allAreSame) {
        return filterByDate(datedValues, UtilDateTime.nowTimestamp(), null, null, allAreSame);
    }

    /**
     * returns the values that are active at the moment.
     * @param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     * @param moment      the moment in question
     * @return List of GenericValue's that are active at the moment
     */
    public static <T extends GenericEntity> List<T> filterByDate(List<T> datedValues, java.util.Date moment) {
        return filterByDate(datedValues, new Timestamp(moment.getTime()), null, null, true);
    }

    /**
     * returns the values that are active at the moment.
     * @param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     * @param moment      the moment in question
     * @return List of GenericValue's that are active at the moment
     */
    public static <T extends GenericEntity> List<T> filterByDate(List<T> datedValues, Timestamp moment) {
        return filterByDate(datedValues, moment, null, null, true);
    }

    /**
     * returns the values that are active at the moment.
     * @param datedValues GenericValue's that have "fromDate" and "thruDate" fields
     * @param moment      the moment in question
     * @param allAreSame  Specifies whether all values in the List are of the same entity; this can help speed things up a fair amount since we
     *                    only have to see if the from and thru date fields are valid once
     * @return List of GenericValue's that are active at the moment
     */
    public static <T extends GenericEntity> List<T> filterByDate(List<T> datedValues, Timestamp moment, String fromDateName,
                                                                 String thruDateName, boolean allAreSame) {
        if (datedValues == null) return null;
        if (moment == null) return datedValues;
        if (fromDateName == null) fromDateName = "fromDate";
        if (thruDateName == null) thruDateName = "thruDate";

        List<T> result = new LinkedList<>();
        Iterator<T> iter = datedValues.iterator();

        if (allAreSame) {
            ModelField fromDateField = null;
            ModelField thruDateField = null;

            if (iter.hasNext()) {
                T datedValue = iter.next();

                fromDateField = datedValue.getModelEntity().getField(fromDateName);
                if (fromDateField == null) {
                    throw new IllegalArgumentException("\"" + fromDateName + "\" is not a field of " + datedValue.getEntityName());
                }
                thruDateField = datedValue.getModelEntity().getField(thruDateName);
                if (thruDateField == null) {
                    throw new IllegalArgumentException("\"" + thruDateName + "\" is not a field of " + datedValue.getEntityName());
                }

                Timestamp fromDate = (Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                Timestamp thruDate = (Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                } // else not active at moment
            }
            while (iter.hasNext()) {
                T datedValue = iter.next();
                Timestamp fromDate = (Timestamp) datedValue.dangerousGetNoCheckButFast(fromDateField);
                Timestamp thruDate = (Timestamp) datedValue.dangerousGetNoCheckButFast(thruDateField);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                } // else not active at moment
            }
        } else {
            // if not all values are known to be of the same entity, must check each one...
            while (iter.hasNext()) {
                T datedValue = iter.next();
                Timestamp fromDate = datedValue.getTimestamp(fromDateName);
                Timestamp thruDate = datedValue.getTimestamp(thruDateName);

                if ((thruDate == null || thruDate.after(moment)) && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment))) {
                    result.add(datedValue);
                } // else not active at moment
            }
        }

        return result;
    }

    public static boolean isValueActive(GenericValue datedValue, Timestamp moment) {
        return isValueActive(datedValue, moment, "fromDate", "thruDate");
    }

    public static boolean isValueActive(GenericValue datedValue, Timestamp moment, String fromDateName, String thruDateName) {
        Timestamp fromDate = datedValue.getTimestamp(fromDateName);
        Timestamp thruDate = datedValue.getTimestamp(thruDateName);
        return (thruDate == null || thruDate.after(moment))
                && (fromDate == null || fromDate.before(moment) || fromDate.equals(moment));
    }

    /**
     * returns the values that match the values in fields
     * @param values List of GenericValues
     * @param fields the field-name/value pairs that must match
     * @return List of GenericValue's that match the values in fields
     */
    public static <T extends GenericEntity> List<T> filterByAnd(List<T> values, Map<String, ? extends Object> fields) {
        if (values == null || UtilValidate.isEmpty(fields)) {
            return values;
        }
        return values.stream().filter(value -> value.matchesFields(fields)).collect(toList());
    }

    /**
     * returns the values that match all of the exprs in list
     * @param values List of GenericValues
     * @param exprs  the expressions that must validate to true
     * @return List of GenericValue's that match the values in fields
     */
    public static <T extends GenericEntity> List<T> filterByAnd(List<T> values, List<? extends EntityCondition> exprs) {
        if (values == null || UtilValidate.isEmpty(exprs)) {
            return values;
        }

        return values.stream()
                .filter(value -> exprs.stream().allMatch(condition -> condition.entityMatches(value)))
                .collect(toList());
    }

    /**
     * returns the values that match any of the exprs in list
     * @param values List of GenericValues
     * @param exprs  the expressions that must validate to true
     * @return List of GenericValue's that match the values in fields
     */
    public static <T extends GenericEntity> List<T> filterByOr(List<T> values, List<? extends EntityCondition> exprs) {
        if (values == null || UtilValidate.isEmpty(exprs)) {
            return values;
        }

        return values.stream()
                .filter(value -> exprs.stream().anyMatch(condition -> condition.entityMatches(value)))
                .collect(toList());
    }

    /**
     * returns the values in the order specified after with localized value
     * @param values  List of GenericValues
     * @param orderBy The fields of the named entity to order the query by;
     *                optionally add a " ASC" for ascending or " DESC" for descending
     * @param locale  Locale use to retrieve localized value
     * @return List of GenericValue's in the proper order
     */
    public static <T extends GenericEntity> List<T> localizedOrderBy(Collection<T> values, List<String> orderBy, Locale locale) {
        if (values == null) return null;
        if (values.isEmpty()) return new ArrayList<>();
        //force check entity label before order by
        List<T> localizedValues = new ArrayList<>();
        for (T value : values) {
            T newValue = UtilGenerics.cast(value.clone());
            for (String orderByField : orderBy) {
                if (orderByField.endsWith(" DESC")) {
                    orderByField = orderByField.substring(0, orderByField.length() - 5);
                } else if (orderByField.endsWith(" ASC")) {
                    orderByField = orderByField.substring(0, orderByField.length() - 4);
                } else if (orderByField.startsWith("-")
                        || orderByField.startsWith("+")) {
                    orderByField = orderByField.substring(1, orderByField.length());
                }
                newValue.put(orderByField, value.get(orderByField, locale));
            }
            localizedValues.add(newValue);
        }
        return orderBy(localizedValues, orderBy);
    }

    /**
     * returns the values in the order specified
     * @param values  List of GenericValues
     * @param orderBy The fields of the named entity to order the query by;
     *                optionally add a " ASC" for ascending or " DESC" for descending
     * @return List of GenericValue's in the proper order
     */
    public static <T extends GenericEntity> List<T> orderBy(Collection<T> values, List<String> orderBy) {
        if (values == null) return null;
        if (values.isEmpty()) return new ArrayList<>();
        if (UtilValidate.isEmpty(orderBy)) {
            List<T> newList = new ArrayList<>();
            newList.addAll(values);
            return newList;
        }

        List<T> result = new ArrayList<>();
        result.addAll(values);
        if (Debug.verboseOn()) {
            Debug.logVerbose("Sorting " + values.size() + " values, orderBy=" + orderBy.toString(), MODULE);
        }
        result.sort(new OrderByList(orderBy));
        return result;
    }

    /**
     * @deprecated use {@link #getRelated(String, Map, List, boolean)}
     */
    @Deprecated
    public static List<GenericValue> getRelated(String relationName, List<GenericValue> values) throws GenericEntityException {
        Debug.logWarning("deprecated method, please replace as suggested in API Java Doc, and link to OFBIZ-6651",
                GenericValue.getStackTraceAsString());
        return getRelated(relationName, null, values, false);
    }

    public static List<GenericValue> getRelated(String relationName, Map<String, ? extends Object> fields, List<GenericValue> values,
                                                boolean useCache) throws GenericEntityException {
        if (values == null) return null;

        List<GenericValue> result = new LinkedList<>();
        for (GenericValue value : values) {
            result.addAll(value.getRelated(relationName, fields, null, useCache));
        }
        return result;
    }

    public static <T extends GenericEntity> List<T> filterByCondition(List<T> values, EntityCondition condition) {
        if (values == null || UtilValidate.isEmpty(condition)) {
            return values;
        }
        return values.stream().filter(condition::entityMatches).collect(toList());
    }

    public static <T extends GenericEntity> List<T> filterOutByCondition(List<T> values, EntityCondition condition) {
        if (values == null || UtilValidate.isEmpty(condition)) {
            return values;
        }
        return values.stream().filter(value -> !condition.entityMatches(value)).collect(toList());
    }

    public static List<GenericValue> findDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> search)
            throws GenericEntityException {
        return findDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static List<GenericValue> findDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> search,
                                                              Timestamp now) throws GenericEntityException {
        EntityCondition searchCondition = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(search), EntityUtil.getFilterByDateExpr(now)));
        return EntityQuery.use(delegator).from(entityName).where(searchCondition).orderBy("-fromDate").queryList();
    }

    public static GenericValue newDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> search)
            throws GenericEntityException {
        return newDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static GenericValue newDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> find, Timestamp now)
            throws GenericEntityException {
        Map<String, Object> search;
        List<GenericValue> entities = findDatedInclusionEntity(delegator, entityName, find, now);
        if (UtilValidate.isNotEmpty(entities)) {
            search = null;
            for (GenericValue entity : entities) {
                if (now.equals(entity.get("fromDate"))) {
                    search = new HashMap<>();
                    for (Map.Entry<String, ? super Object> entry : entity.getPrimaryKey().entrySet()) {
                        search.put(entry.getKey(), entry.getValue());
                    }
                    entity.remove("thruDate");
                } else {
                    entity.set("thruDate", now);
                }
                entity.store();
            }
            if (search == null) {
                search = new HashMap<>();
                search.putAll(EntityUtil.getFirst(entities));
            }
        } else {
            /* why is this being done? leaving out for now...
            search = new HashMap(search);
            */
            search = new HashMap<>();
            search.putAll(find);
        }
        if (now.equals(search.get("fromDate"))) {
            return EntityUtil.getOnly(EntityQuery.use(delegator).from(entityName).where(search).queryList());
        } else {
            search.put("fromDate", now);
            search.remove("thruDate");
            return delegator.makeValue(entityName, search);
        }
    }

    public static void delDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> search)
            throws GenericEntityException {
        delDatedInclusionEntity(delegator, entityName, search, UtilDateTime.nowTimestamp());
    }

    public static void delDatedInclusionEntity(Delegator delegator, String entityName, Map<String, ? extends Object> search, Timestamp now)
            throws GenericEntityException {
        List<GenericValue> entities = findDatedInclusionEntity(delegator, entityName, search, now);
        for (GenericValue entity : entities) {
            entity.set("thruDate", now);
            entity.store();
        }
    }

    public static <T> List<T> getFieldListFromEntityList(List<GenericValue> genericValueList, String fieldName, boolean distinct) {
        if (genericValueList == null || fieldName == null) {
            return null;
        }

        Stream<T> fieldListStream = genericValueList.stream().map(genericValue -> UtilGenerics.cast(genericValue.get(fieldName)));
        if (distinct) {
            return fieldListStream.distinct().collect(toList());
        } else {
            return fieldListStream.collect(toList());
        }
    }

    public static <T> List<T> getFieldListFromEntityListIterator(EntityListIterator genericValueEli, String fieldName, boolean distinct) {
        if (genericValueEli == null || fieldName == null) {
            return null;
        }
        List<T> fieldList = new LinkedList<>();
        Set<T> distinctSet = null;
        if (distinct) {
            distinctSet = new HashSet<>();
        }

        GenericValue value = null;
        while ((value = genericValueEli.next()) != null) {
            T fieldValue = UtilGenerics.<T>cast(value.get(fieldName));
            if (fieldValue != null) {
                if (distinct) {
                    if (!distinctSet.contains(fieldValue)) {
                        fieldList.add(fieldValue);
                        distinctSet.add(fieldValue);
                    }
                } else {
                    fieldList.add(fieldValue);
                }
            }
        }

        return fieldList;
    }

    /**
     * Returns <code>true</code> if multi-tenant has been enabled.
     * <p>Multi-tenant features are enabled by setting the <code>multitenant</code>
     * property in <code>general.properties</code> to "Y".</p>
     */
    public static boolean isMultiTenantEnabled() {
        return "Y".equalsIgnoreCase(UtilProperties.getPropertyValue("general", "multitenant"));
    }

    /**
     * @param viewIndex
     * @param viewSize
     * @return the calculated start index based on viewIndex and viewSize
     * @see EntityUtil#getPagedList
     */
    public static int getStartIndexFromViewIndex(int viewIndex, int viewSize) {
        return viewIndex == 0 ? 1 : (viewIndex * viewSize) + 1;
    }

    /**
     * @param iter      EntityListIterator
     * @param viewIndex
     * @param viewSize
     * @return PagedList object with a subset of data items from EntityListIterator based on viewIndex and viewSize
     * @throws GenericEntityException
     * @see org.sitenetsoft.sunseterp.framework.entity.util.EntityListIterator
     */
    public static PagedList<GenericValue> getPagedList(EntityListIterator iter, int viewIndex, int viewSize) throws GenericEntityException {
        int startIndex = getStartIndexFromViewIndex(viewIndex, viewSize);
        int endIndex = (startIndex + viewSize) - 1;

        List<GenericValue> dataItems = iter.getPartialList(startIndex, viewSize);
        if (dataItems.size() < viewIndex) {
            endIndex = (endIndex - viewSize) + dataItems.size();
        }

        int size = iter.getResultsSizeAfterPartialList();
        if (endIndex > size) {
            endIndex = size;
        }

        return new PagedList<>(startIndex, endIndex, size, viewIndex, viewSize, dataItems);
    }

    /**
     * For a entityName return the primary keys path that identify it
     * like entityName/pkValue1/pkValue2/../pkValueN
     * @param delegator
     * @param entityName
     * @param context
     * @return
     */
    public static String entityToPath(Delegator delegator, String entityName, Map<String, Object> context) {
        return entityToPath(delegator.makeValidValue(entityName, context));
    }

    /**
     * For a entityName return the primary keys path that identify it
     * like entityName/pkValue1/pkValue2/../pkValueN
     * @param gv
     * @return
     */
    public static String entityToPath(GenericValue gv) {
        StringBuilder path = new StringBuilder(gv.getEntityName());
        for (String pkName : gv.getModelEntity().getPkFieldNames()) {
            path.append("/").append(gv.getString(pkName));
        }
        return path.toString();
    }

    /**
     * Form a entityName and primary keys path
     * convert it to a Map contains all pkValue :
     * entityName/pkValue1/pkValue2/../pkValueN
     * -&gt; [pkName1: pkValue1,
     * pkName2, pkValue2,
     * ...,
     * pkNameN: pkValueN]
     * @param modelEntity
     * @param path
     * @return
     */
    public static Map<String, Object> getPkValuesMapFromPath(ModelEntity modelEntity, String path)
            throws GenericEntityException {
        if (UtilValidate.isEmpty(path)) return null;
        LinkedList<String> pkValues = new LinkedList<>(Arrays.asList(path.split("/")));
        List<String> pkFieldNames = modelEntity.getPkFieldNames();
        if (pkFieldNames.size() != pkValues.size()) {
            throw new GenericEntityException("Identification path failed ");
        }
        Map<String, Object> pkValuesMap = new HashMap<>();
        for (String pkName : modelEntity.getPkFieldNames()) {
            pkValuesMap.put(pkName, pkValues.removeFirst());
        }
        return pkValuesMap;
    }
}
