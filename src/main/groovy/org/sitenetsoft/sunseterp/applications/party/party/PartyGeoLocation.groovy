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
package org.sitenetsoft.sunseterp.applications.party.party

import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties
import org.sitenetsoft.sunseterp.framework.common.geo.GeoWorker

uiLabelMap = UtilProperties.getResourceBundleMap('PartyUiLabels', locale)
uiLabelMap.addBottomResourceBundle('CommonUiLabels')

partyId = parameters.partyId ?: parameters.party_id
userLoginId = parameters.userlogin_id ?: parameters.userLoginId

if (!partyId && userLoginId) {
    thisUserLogin = from('UserLogin').where('userLoginId', userLoginId).queryOne()
    if (thisUserLogin) {
        partyId = thisUserLogin.partyId
    }
}
geoPointId = parameters.geoPointId
context.partyId = partyId

if (geoPointId) {
    latestGeoPoint = from('GeoPoint').where('geoPointId', geoPointId).queryOne()
} else {
    latestGeoPoint = GeoWorker.findLatestGeoPoint(delegator, 'PartyAndGeoPoint', 'partyId', partyId, null, null)
}
if (latestGeoPoint) {
    context.latestGeoPoint = latestGeoPoint

    if (latestGeoPoint && latestGeoPoint.containsKey('latitude') && latestGeoPoint.containsKey('longitude')) {
        List geoPoints = UtilMisc.toList(UtilMisc.toMap('lat', latestGeoPoint.latitude, 'lon', latestGeoPoint.longitude, 'partyId', partyId,
              'link', [url: 'viewprofile?partyId=' + partyId, label: uiLabelMap.PartyProfile + ' ' + uiLabelMap.CommonOf + ' ' + partyId]))

        Map geoChart = [width: '500px', height: '450px', controlUI: 'small', dataSourceId: latestGeoPoint.dataSourceId, points: geoPoints]
        context.geoChart = geoChart
    }
    if (latestGeoPoint && latestGeoPoint.elevationUomId) {
        elevationUom = from('Uom').where('uomId', latestGeoPoint.elevationUomId).queryOne()
        context.elevationUomAbbr = elevationUom.abbreviation
    }
}
