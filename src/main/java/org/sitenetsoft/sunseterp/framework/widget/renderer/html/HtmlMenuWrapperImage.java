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
package org.sitenetsoft.sunseterp.framework.widget.renderer.html;

import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.widget.model.ModelMenuItem;
import org.sitenetsoft.sunseterp.framework.widget.renderer.MenuStringRenderer;
import org.xml.sax.SAXException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Widget Library - HTML Menu Wrapper class - makes it easy to do the setup and render of a menu
 *
 * @deprecated since 2021-01-14
 */
@Deprecated
public class HtmlMenuWrapperImage extends HtmlMenuWrapper {

    private static final String MODULE = HtmlMenuWrapperImage.class.getName();

    protected HtmlMenuWrapperImage() { }

    public HtmlMenuWrapperImage(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {
        super(resourceName, menuName, request, response);
    }

    @Override
    public MenuStringRenderer getMenuRenderer() {
        return new HtmlMenuRendererImage(getRequest(), getResponse());
    }

    @Override
    public void init(String resourceName, String menuName, HttpServletRequest request, HttpServletResponse response)
            throws IOException, SAXException, ParserConfigurationException {

        super.init(resourceName, menuName, request, response);
        Map<String, Object> dummyMap = new HashMap<>();
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        try {
            for (ModelMenuItem menuItem : getModelMenu().getMenuItemList()) {
                String contentId = menuItem.getAssociatedContentId(dummyMap);
                GenericValue webSitePublishPoint =
                        EntityQuery.use(delegator).from("WebSitePublishPoint").where("contentId", contentId).cache().queryOne();
                String menuItemName = menuItem.getName();
                putInContext(menuItemName, "WebSitePublishPoint", webSitePublishPoint);
            }
        } catch (GenericEntityException e) {
            return;
        }
    }
}
