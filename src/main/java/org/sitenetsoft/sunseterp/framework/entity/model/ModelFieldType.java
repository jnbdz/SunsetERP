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
package org.sitenetsoft.sunseterp.framework.entity.model;

import org.sitenetsoft.sunseterp.framework.base.util.UtilXml;
import org.sitenetsoft.sunseterp.framework.entity.jdbc.JdbcValueHandler;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Locale;

/**
 * Generic Entity - FieldType model class
 *
 */

@SuppressWarnings("serial")
public class ModelFieldType implements Serializable {

    private static final String MODULE = ModelFieldType.class.getName();

    /** The type of the Field */
    private String type;
    /** The java-type of the Field */
    private String javaType;
    /** The JDBC value handler for this Field */
    private JdbcValueHandler<?> jdbcValueHandler;
    /** The sql-type of the Field */
    private String sqlType;
    /** The sql-type-alias of the Field, this is optional */
    private String sqlTypeAlias;

    /** XML Constructor */
    public ModelFieldType(Element fieldTypeElement) {
        this.type = UtilXml.checkEmpty(fieldTypeElement.getAttribute("type")).intern();
        this.javaType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("java-type")).intern();
        this.sqlType = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type")).intern();
        this.sqlTypeAlias = UtilXml.checkEmpty(fieldTypeElement.getAttribute("sql-type-alias")).intern();
        this.jdbcValueHandler = JdbcValueHandler.getInstance(this.javaType, this.sqlType);
    }

    /** The type of the Field */
    public String getType() {
        return this.type;
    }

    /** The java-type of the Field */
    public String getJavaType() {
        return this.javaType;
    }

    /** Returns the JDBC value handler for this field type */
    public JdbcValueHandler<?> getJdbcValueHandler() {
        return this.jdbcValueHandler;
    }

    /** The sql-type of the Field */
    public String getSqlType() {
        return this.sqlType;
    }

    /** The sql-type-alias of the Field */
    public String getSqlTypeAlias() {
        return this.sqlTypeAlias;
    }

    /** A simple function to derive the max length of a String created from the field value, based on the sql-type
     * @return max length of a String representing the Field value
     */
    public int stringLength() {
        String sqlTypeUpperCase = sqlType.toUpperCase(Locale.getDefault());
        if (sqlTypeUpperCase.indexOf("VARCHAR") >= 0) {
            if (sqlTypeUpperCase.indexOf('(') > 0 && sqlTypeUpperCase.indexOf(')') > 0) {
                String length = sqlTypeUpperCase.substring(sqlTypeUpperCase.indexOf('(') + 1, sqlTypeUpperCase.indexOf(')'));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlTypeUpperCase.indexOf("CHAR") >= 0) {
            if (sqlTypeUpperCase.indexOf('(') > 0 && sqlTypeUpperCase.indexOf(')') > 0) {
                String length = sqlTypeUpperCase.substring(sqlTypeUpperCase.indexOf('(') + 1, sqlTypeUpperCase.indexOf(')'));

                return Integer.parseInt(length);
            } else {
                return 255;
            }
        } else if (sqlTypeUpperCase.indexOf("TEXT") >= 0 || sqlTypeUpperCase.indexOf("LONG") >= 0 || sqlTypeUpperCase.indexOf("CLOB") >= 0) {
            return 5000;
        }
        return 20;
    }
}
