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
package org.sitenetsoft.sunseterp.framework.widget.model;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilGenerics;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.util.Map;

/**
 * Widget Library - Widget model class. ModelWidget is a base class that is
 * extended by other widget model classes.
 */
@SuppressWarnings("serial")
public abstract class ModelWidget implements Serializable {

    private static final String MODULE = ModelWidget.class.getName();
    /**
     * The parameter name used to control widget boundary comments. Currently
     * set to "widgetVerbose".
     */
    public static final String ENABLE_BOUNDARY_COMMENTS_PARAM = "widgetVerbose";
    public enum NamedBorderType { NONE, LABEL, SOURCE }

    private final String name;
    private final String systemId;
    private final int startColumn;
    private final int startLine;

    /**
     * Derived classes must call this constructor.
     * @param name The widget name
     */
    protected ModelWidget(String name) {
        this.name = name;
        this.systemId = "anonymous";
        this.startColumn = 0;
        this.startLine = 0;
    }

    /**
     * Derived classes must call this constructor.
     * @param widgetElement The XML Element for the widget
     */
    protected ModelWidget(Element widgetElement) {
        this.name = widgetElement.getAttribute("name");
        this.systemId = (String) widgetElement.getUserData("systemId");
        this.startColumn = (Integer) widgetElement.getUserData("startColumn");
        this.startLine = (Integer) widgetElement.getUserData("startLine");
    }

    public abstract void accept(ModelWidgetVisitor visitor) throws Exception;

    /**
     * Returns the widget's name.
     * @return Widget's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the url as a string, from where this widget was defined.
     * @return url
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the column where this widget was defined, in it's containing xml file.
     * @return start column
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * Returns the line where this widget was defined, in it's containing xml file.
     * @return start line
     */
    public int getStartLine() {
        return startLine;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        ModelWidgetVisitor visitor = new XmlWidgetVisitor(sb);
        try {
            accept(visitor);
        } catch (Exception e) {
            Debug.logWarning(e, "Exception thrown in XmlWidgetVisitor: ", MODULE);
        }
        return sb.toString();
    }

    /**
     * Returns the widget's name to be used in boundary comments. The default action
     * is to return the widget's name. Derived classes can override this method to
     * return a customized name.
     * @return Name to be used in boundary comments
     */
    public String getBoundaryCommentName() {
        return name;
    }

    /**
     * Returns <code>true</code> if widget boundary comments are enabled. Widget boundary comments are
     * enabled by setting <code>widget.verbose=true</code> in the <code>widget.properties</code> file.
     * The <code>true</code> setting can be overridden in <code>web.xml</code> or in the screen
     * rendering context. If <code>widget.verbose</code> is set to <code>false</code> in the
     * <code>widget.properties</code> file, then that setting will override all other settings and
     * disable all widget boundary comments.
     * @param context Optional context Map
     */
    public static boolean widgetBoundaryCommentsEnabled(Map<String, ? extends Object> context) {
        boolean result = "true".equals(UtilProperties.getPropertyValue("widget", "widget.verbose"));
        if (result && context != null) {
            String str = (String) context.get(ENABLE_BOUNDARY_COMMENTS_PARAM);
            if (str != null) {
                result = "true".equals(str);
            } else {
                Map<String, ? extends Object> parameters = UtilGenerics.cast(context.get("parameters"));
                if (parameters != null) {
                    str = (String) parameters.get(ENABLE_BOUNDARY_COMMENTS_PARAM);
                    if (str != null) {
                        result = "true".equals(str);
                    }
                }
            }
        }
        return result;
    }

    /**
     * determine how to display named border for development
     * @return NamedBorderType from <code>widget.dev.namedBorder</code> property
     */
    public static NamedBorderType widgetNamedBorderType() {
        return NamedBorderType.valueOf(UtilProperties.getPropertyValue("widget", "widget.dev.namedBorder", NamedBorderType.NONE.toString()));
    }
}
