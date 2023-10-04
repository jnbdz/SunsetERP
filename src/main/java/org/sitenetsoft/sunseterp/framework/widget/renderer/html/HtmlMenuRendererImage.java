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

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.widget.content.WidgetContentWorker;
import org.sitenetsoft.sunseterp.framework.widget.model.ModelMenuItem;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * Widget Library - HTML Menu Renderer implementation
 *
 * @deprecated since 2021-01-14
 */
@Deprecated
public class HtmlMenuRendererImage extends HtmlMenuRenderer {

    protected HtmlMenuRendererImage() { }

    public HtmlMenuRendererImage(HttpServletRequest request, HttpServletResponse response) {
        super(request, response);
    }

    /**
     * Build div str string.
     * @param menuItem the menu item
     * @param context  the context
     * @return the string
     * @throws IOException the io exception
     */
    public String buildDivStr(ModelMenuItem menuItem, Map<String, Object> context) throws IOException {

        StringBuilder imgStr = new StringBuilder("<img src=\"");
        String contentId = menuItem.getAssociatedContentId(context);
        Delegator delegator = (Delegator) getRequest().getAttribute("delegator");
        GenericValue webSitePublishPoint = null;
        try {
            if (WidgetContentWorker.getContentWorker() != null) {
                webSitePublishPoint = WidgetContentWorker.getContentWorker().getWebSitePublishPointExt(delegator, contentId, false);
            } else {
                Debug.logError("Not rendering image because can't get WebSitePublishPoint, not ContentWorker found.", MODULE);
            }
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }
        String medallionLogoStr = webSitePublishPoint.getString("medallionLogo");
        StringWriter buf = new StringWriter();
        appendContentUrl(buf, medallionLogoStr);
        imgStr.append(buf.toString());
        String cellWidth = menuItem.getCellWidth();
        imgStr.append("\"");
        if (UtilValidate.isNotEmpty(cellWidth)) {
            imgStr.append(" width=\"").append(cellWidth).append("\" ");
        }

        imgStr.append(" border=\"0\" />");
        return imgStr.toString();
    }

}
