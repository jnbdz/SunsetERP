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

package org.sitenetsoft.sunseterp.framework.entity.condition;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntity;
import org.sitenetsoft.sunseterp.framework.entity.GenericModelException;
import org.sitenetsoft.sunseterp.framework.entity.config.model.Datasource;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelField;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelViewEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelViewEntity.ModelAlias;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.sitenetsoft.sunseterp.framework.entity.condition.EntityConditionUtils.getField;

/**
 * Field value expression.
 *
 */
@SuppressWarnings("serial")
public class EntityFieldValue extends EntityConditionValue {

    private static final String MODULE = EntityFieldValue.class.getName();

    private String fieldName = null;
    private String entityAlias = null;
    private List<String> entityAliasStack = null;
    private ModelViewEntity modelViewEntity = null;

    public static EntityFieldValue makeFieldValue(String fieldName) {
        EntityFieldValue efv = new EntityFieldValue();
        efv.init(fieldName, null, null, null);
        return efv;
    }

    public static EntityFieldValue makeFieldValue(String fieldName, String entityAlias, List<String> entityAliasStack,
                                                  ModelViewEntity modelViewEntity) {
        EntityFieldValue efv = new EntityFieldValue();
        efv.init(fieldName, entityAlias, entityAliasStack, modelViewEntity);
        return efv;
    }

    /**
     * Init.
     * @param fieldName        the field name
     * @param entityAlias      the entity alias
     * @param entityAliasStack the entity alias stack
     * @param modelViewEntity  the model view entity
     */
    public void init(String fieldName, String entityAlias, List<String> entityAliasStack, ModelViewEntity modelViewEntity) {
        this.fieldName = fieldName;
        this.entityAlias = entityAlias;
        if (UtilValidate.isNotEmpty(entityAliasStack)) {
            this.entityAliasStack = new LinkedList<>();
            this.entityAliasStack.addAll(entityAliasStack);
        }
        this.modelViewEntity = modelViewEntity;
        if (UtilValidate.isNotEmpty(this.entityAliasStack) && UtilValidate.isEmpty(this.entityAlias)) {
            // look it up on the view entity so it can be part of the big list, this only happens for aliased fields, so find
            // the entity-alias and field-name for the alias
            ModelAlias modelAlias = this.modelViewEntity.getAlias(this.fieldName);
            if (modelAlias != null) {
                this.entityAlias = modelAlias.getEntityAlias();
                this.fieldName = modelAlias.getField();
            }
            // TODO/NOTE: this will ignore function, group-by, etc... should maybe support those in conditions too at some point
        }
    }

    /**
     * Reset.
     */
    public void reset() {
        this.fieldName = null;
        this.entityAlias = null;
        this.entityAliasStack = null;
        this.modelViewEntity = null;
    }

    /**
     * Gets field name.
     * @return the field name
     */
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int hashCode() {
        int hash = fieldName.hashCode();
        if (this.entityAlias != null) {
            hash |= this.entityAlias.hashCode();
        }
        if (this.entityAliasStack != null) {
            hash |= this.entityAliasStack.hashCode();
        }
        if (this.modelViewEntity != null) {
            hash |= this.modelViewEntity.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityFieldValue)) {
            return false;
        }
        EntityFieldValue otherValue = (EntityFieldValue) obj;
        return fieldName.equals(otherValue.fieldName) && !(UtilMisc.compare(this.entityAlias, otherValue.entityAlias) != 0)
                && !(UtilMisc.compare(this.entityAliasStack, otherValue.entityAliasStack) != 0);
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        if (this.modelViewEntity != null) {
            if (this.entityAlias != null) {
                ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(entityAlias);
                return getField(memberModelEntity, fieldName);
            }
            return getField(modelViewEntity, fieldName);
        }
        return getField(modelEntity, fieldName);
    }


    @Override
    public void setModelField(ModelField field) {
        Debug.logInfo("Logging to avoid checkstyle issue.", MODULE);
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity modelEntity,
                            List<EntityConditionParam> entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceInfo) {
        if (this.modelViewEntity != null) {
            // NOTE: this section is a bit of a hack; the other code is terribly complex and really needs to be refactored to
            // incorporate support for this

            if (UtilValidate.isNotEmpty(entityAlias)) {
                ModelEntity memberModelEntity = modelViewEntity.getMemberModelEntity(entityAlias);
                ModelField modelField = memberModelEntity.getField(fieldName);

                // using entityAliasStack (ordered top to bottom) build a big long alias; not that dots will be replaced after it is
                // combined with the column name in the SQL gen
                if (UtilValidate.isNotEmpty(this.entityAliasStack)) {
                    boolean dotUsed = false;
                    for (String curEntityAlias: entityAliasStack) {
                        sql.append(curEntityAlias);
                        if (dotUsed) {
                            sql.append("_");
                        } else {
                            sql.append(".");
                            dotUsed = true;
                        }

                    }
                    sql.append(entityAlias);
                    sql.append("_");
                    sql.append(modelField.getColName());
                } else {
                    sql.append(entityAlias);
                    sql.append(".");
                    sql.append(modelField.getColName());
                }
            } else {
                sql.append(getColName(tableAliases, modelViewEntity, fieldName, includeTableNamePrefix, datasourceInfo));
            }
        } else {
            sql.append(getColName(tableAliases, modelEntity, fieldName, includeTableNamePrefix, datasourceInfo));
        }
    }

    private static String getColName(Map<String, String> tableAliases, ModelEntity modelEntity, String fieldName,
            boolean includeTableNamePrefix, Datasource datasourceInfo) {
        if (modelEntity == null) {
            return fieldName;
        }
        return getColName(tableAliases, modelEntity, getField(modelEntity, fieldName), fieldName,
                includeTableNamePrefix, datasourceInfo);
    }

    private static String getColName(Map<String, String> tableAliases, ModelEntity modelEntity, ModelField modelField,
            String fieldName, boolean includeTableNamePrefix, Datasource datasourceInfo) {
        if (modelEntity == null || modelField == null) {
            return fieldName;
        }

        // If this is a view entity and we are configured to alias the views, use the alias here
        // instead of the composite (i.e. table.column) field name.
        if (datasourceInfo != null && datasourceInfo.getAliasViewColumns() && modelEntity instanceof ModelViewEntity) {
            ModelViewEntity modelViewEntity = (ModelViewEntity) modelEntity;
            ModelAlias modelAlias = modelViewEntity.getAlias(fieldName);
            if (modelAlias != null) {
                return modelAlias.getColAlias();
            }
        }

        String colName = getColName(modelField, fieldName);
        if (includeTableNamePrefix && datasourceInfo != null) {
            String tableName = modelEntity.getTableName(datasourceInfo);
            if (tableAliases.containsKey(tableName)) {
                tableName = tableAliases.get(tableName);
            }
            colName = tableName + "." + colName;
        }
        return colName;
    }

    private static String getColName(ModelField modelField, String fieldName) {
        return (modelField == null) ? fieldName : modelField.getColValue();
    }

    @Override
    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        ModelField field = getModelField(modelEntity);
        if (field == null) {
            throw new GenericModelException("Field with name " + fieldName + " not found in the " + modelEntity.getEntityName() + " Entity");
        }
    }

    @Override
    public Object getValue(Delegator delegator, Map<String, ? extends Object> map) {
        if (map == null) {
            return null;
        }
        if (map instanceof GenericEntity.NULL) {
            return null;
        }
        return map.get(fieldName);
    }

    @Override
    public EntityConditionValue freeze() {
        return this;
    }
}
