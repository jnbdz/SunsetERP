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
package org.sitenetsoft.sunseterp.applications.content.layout

import org.sitenetsoft.sunseterp.framework.base.util.UtilHttp
import org.sitenetsoft.sunseterp.framework.base.util.template.FreeMarkerWorker
import org.sitenetsoft.sunseterp.applications.content.data.DataResourceWorker
import org.sitenetsoft.sunseterp.framework.webapp.ftl.FreeMarkerViewHandler

import freemarker.template.WrappingTemplateModel

Locale locale = UtilHttp.getLocale(request)
if (currentValue) {
    dataResourceId = currentValue.drDataResourceId
    dataResourceTypeId = currentValue.drDataResourceTypeId
    if (dataResourceTypeId) {
        mimeTypeId = currentValue.drMimeTypeId
        rootDir = request.getSession().getServletContext().getRealPath('/')
        wrapper = FreeMarkerWorker.getDefaultOfbizWrapper()
        WrappingTemplateModel.setDefaultObjectWrapper(wrapper)
        templateRoot = [:]
        FreeMarkerViewHandler.prepOfbizRoot(templateRoot, request, response)
        ctx = [:]
        ctx.rootDir = rootDir
        // webSiteId and https need to go here, too
        templateRoot.context = ctx
        currentValue.drDataTemplateTypeId = 'NONE'
        textData = DataResourceWorker.renderDataResourceAsText(dispatcher, delegator, dataResourceId, templateRoot, locale, mimeTypeId, false)
        context.textData = textData
    }
}
