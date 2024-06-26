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
package org.sitenetsoft.sunseterp.framework.webtools.service

import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties
import org.sitenetsoft.sunseterp.framework.service.ServiceDispatcher
import org.sitenetsoft.sunseterp.framework.service.engine.GenericEngine

uiLabelMap = UtilProperties.getResourceBundleMap('WebtoolsUiLabels', locale)
uiLabelMap.addBottomResourceBundle('CommonUiLabels')

log = ServiceDispatcher.getServiceLogMap()
serviceList = []
log.each { rs, value ->
    service = [:]
    service.serviceName = rs.getModelService().getName()
    service.localName = rs.getLocalName()
    service.startTime = rs.getStartStamp()
    service.endTime = rs.getEndStamp()
    service.modeStr = rs.getMode() == GenericEngine.SYNC_MODE ? uiLabelMap.WebtoolsSync : uiLabelMap.WebtoolsAsync

    serviceList.add(service)
}
sortField = parameters.sortField
if (sortField) {
    context.services = UtilMisc.sortMaps(serviceList, UtilMisc.toList(sortField))
} else {
    context.services = serviceList
}
