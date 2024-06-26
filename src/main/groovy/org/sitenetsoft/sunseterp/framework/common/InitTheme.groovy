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
package org.sitenetsoft.sunseterp.framework.common

import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc
import org.sitenetsoft.sunseterp.framework.widget.model.ModelTheme
import org.sitenetsoft.sunseterp.framework.widget.model.ThemeFactory
import org.sitenetsoft.sunseterp.framework.widget.renderer.VisualTheme

String visualThemeId = context.visualThemeId ?: parameters?.visualThemeId
VisualTheme visualTheme
if (visualThemeId) {
    visualTheme = ThemeFactory.getVisualThemeFromId(visualThemeId)
} else {
    visualTheme = ThemeFactory.resolveVisualTheme(context.request ?: null)
}

if (visualTheme) {
    ModelTheme modelTheme = visualTheme.getModelTheme()
    globalContext.commonScreenLocations = modelTheme.getModelCommonScreens()
    globalContext.commonFormLocations = modelTheme.getModelCommonForms()
    globalContext.visualTheme = visualTheme
    globalContext.modelTheme = modelTheme

    if (globalContext.layoutSettings && globalContext.layoutSettings instanceof Map) {
        globalContext.layoutSettings.putAll(modelTheme.getThemeResources())
    } else {
        globalContext.layoutSettings = UtilMisc.makeMapWritable(modelTheme.getThemeResources())
    }
}
context.globalContext = globalContext
