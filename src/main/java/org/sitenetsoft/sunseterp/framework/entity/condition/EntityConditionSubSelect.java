/*
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
 */
package org.sitenetsoft.sunseterp.framework.entity.condition;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericModelException;
import org.sitenetsoft.sunseterp.framework.entity.config.model.Datasource;
import org.sitenetsoft.sunseterp.framework.entity.jdbc.SqlJdbcUtil;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelField;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelViewEntity;

import java.util.List;
import java.util.Map;

/**
 * Sub-query action.
 *
 */
@SuppressWarnings("serial")
public class EntityConditionSubSelect extends EntityConditionValue {
    private static final String MODULE = EntityConditionSubSelect.class.getName();

    private ModelEntity localModelEntity = null;
    private String keyFieldName = null;
    private EntityCondition whereCond = null;
    private Boolean requireAll = null;

    protected EntityConditionSubSelect() { }

    public EntityConditionSubSelect(String entityName, String keyFieldName, EntityCondition whereCond, boolean requireAll, Delegator delegator) {
        this(delegator.getModelEntity(entityName), keyFieldName, whereCond, requireAll);
    }
    public EntityConditionSubSelect(ModelEntity localModelEntity, String keyFieldName, EntityCondition whereCond, boolean requireAll) {
        this.localModelEntity = localModelEntity;
        this.keyFieldName = keyFieldName;
        this.whereCond = whereCond;
        this.requireAll = requireAll;
    }

    @Override
    public void addSqlValue(StringBuilder sql, Map<String, String> tableAliases, ModelEntity parentModelEntity, List<EntityConditionParam>
            entityConditionParams, boolean includeTableNamePrefix, Datasource datasourceInfo) {
        if (localModelEntity instanceof ModelViewEntity && datasourceInfo == null) {
            throw new IllegalArgumentException("Call to EntityConditionSubSelect.addSqlValue with datasourceInfo=null which is not allowed "
                    + "because the local entity [" + this.localModelEntity.getEntityName() + "] is a view entity");
        }
        try {
            // add select and where and such, based on local entity not on the main entity
            ModelField localModelField = localModelEntity.getField(this.keyFieldName);

            if (this.requireAll) {
                sql.append(" ALL(");
            } else {
                sql.append(" ANY(");
            }
            sql.append("SELECT ");

            sql.append(localModelField.getColName());

            // FROM clause and when necessary the JOIN or LEFT JOIN clause(s) as well
            sql.append(SqlJdbcUtil.makeFromClause(localModelEntity, null, datasourceInfo));

            // WHERE clause
            StringBuilder whereString = new StringBuilder();
            String entityCondWhereString = "";
            if (this.whereCond != null) {
                entityCondWhereString = this.whereCond.makeWhereString(localModelEntity, entityConditionParams, datasourceInfo);
            }

            String viewClause = SqlJdbcUtil.makeViewWhereClause(localModelEntity, (datasourceInfo != null ? datasourceInfo.getJoinStyle() : null));
            if (!viewClause.isEmpty()) {
                if (!entityCondWhereString.isEmpty()) {
                    whereString.append("(");
                    whereString.append(entityCondWhereString);
                    whereString.append(") AND ");
                }

                whereString.append(viewClause);
            } else {
                whereString.append(entityCondWhereString);
            }

            if (whereString.length() > 0) {
                sql.append(" WHERE ");
                sql.append(whereString.toString());
            }

            sql.append(")");
        } catch (GenericEntityException e) {
            String errMsg = "Could not generate sub-select SQL: " + e.toString();
            Debug.logError(e, errMsg, MODULE);

        }
    }


    @Override
    public EntityConditionValue freeze() {
        return new EntityConditionSubSelect(localModelEntity, keyFieldName, (whereCond != null ? whereCond.freeze() : null), requireAll);
    }

    /**
     * Gets key field name.
     * @return the key field name
     */
    public String getKeyFieldName() {
        return this.keyFieldName;
    }

    /**
     * Gets model entity.
     * @return the model entity
     */
    public ModelEntity getModelEntity() {
        return this.localModelEntity;
    }

    @Override
    public ModelField getModelField(ModelEntity modelEntity) {
        return null;
    }

    @Override
    public void setModelField(ModelField modelEntity) {
        Debug.logInfo("Logging to avoid checkstyle issue.", MODULE);
    }

    @Override
    public Comparable<?> getValue(Delegator delegator, Map<String, ? extends Object> map) {
        return null;
    }

    @Override
    public void validateSql(ModelEntity modelEntity) throws GenericModelException {
        Debug.logInfo("Logging to avoid checkstyle issue.", MODULE);
    }
}
