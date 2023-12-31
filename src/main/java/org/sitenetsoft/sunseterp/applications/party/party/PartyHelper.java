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

package org.sitenetsoft.sunseterp.applications.party.party;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilFormatOut;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;

/**
 * PartyHelper
 */
public final class PartyHelper {

    private static final String MODULE = PartyHelper.class.getName();

    private PartyHelper() { }

    public static String getPartyName(GenericValue partyObject) {
        return getPartyName(partyObject, false);
    }

    public static String getPartyName(Delegator delegator, String partyId, boolean lastNameFirst) {
        GenericValue partyObject = null;
        try {
            partyObject = EntityQuery.use(delegator).from("PartyNameView")
                    .where("partyId", partyId)
                    .cache()
                    .queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, "Error finding PartyNameView in getPartyName", MODULE);
        }
        if (partyObject == null) {
            return partyId;
        }
        return formatPartyNameObject(partyObject, lastNameFirst);
    }

    public static String getPartyName(GenericValue partyObject, boolean lastNameFirst) {
        if (partyObject == null) {
            return "";
        }
        if ("PartyGroup".equals(partyObject.getEntityName()) || "Person".equals(partyObject.getEntityName())) {
            return formatPartyNameObject(partyObject, lastNameFirst);
        }
        String partyId = null;
        try {
            partyId = partyObject.getString("partyId");
        } catch (IllegalArgumentException e) {
            Debug.logError(e, "Party object does not contain a party ID", MODULE);
        }

        if (partyId == null) {
            Debug.logWarning("No party ID found; cannot get name based on entity: " + partyObject.getEntityName(), MODULE);
            return "";
        }
        return getPartyName(partyObject.getDelegator(), partyId, lastNameFirst);
    }

    public static String formatPartyNameObject(GenericValue partyValue, boolean lastNameFirst) {
        if (partyValue == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        ModelEntity modelEntity = partyValue.getModelEntity();
        if (modelEntity.isField("firstName") && modelEntity.isField("middleName") && modelEntity.isField("lastName")) {
            if (lastNameFirst) {
                if (!UtilFormatOut.checkNull(partyValue.getString("lastName")).isEmpty()) {
                    result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
                    if (partyValue.getString("firstName") != null) {
                        result.append(", ");
                    }
                }
                result.append(UtilFormatOut.checkNull(partyValue.getString("firstName")));
                if (partyValue.getString("middleName") != null) {
                    result.append(" ");
                }
                result.append(UtilFormatOut.checkNull(partyValue.getString("middleName")));
            } else {
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("firstName"), "", " "));
                result.append(UtilFormatOut.ifNotEmpty(partyValue.getString("middleName"), "", " "));
                result.append(UtilFormatOut.checkNull(partyValue.getString("lastName")));
            }
        }
        if (modelEntity.isField("groupName") && partyValue.get("groupName") != null) {
            result.append(partyValue.getString("groupName"));
        }
        return result.toString();
    }
}
