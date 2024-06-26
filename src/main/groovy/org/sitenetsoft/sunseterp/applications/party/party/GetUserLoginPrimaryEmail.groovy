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

//figure out the PRIMARY_EMAIL of the logged in user, for setting in the send email link
//maybe nice to put in some secondary emails later
if (userLogin) {
    userLoginParty = userLogin.getRelatedOne('Party', true)
    if (userLoginParty) {
        userLoginPartyPrimaryEmails = userLoginParty.getRelated('PartyContactMechPurpose', [contactMechPurposeTypeId: 'PRIMARY_EMAIL'], null, false)
        if (userLoginPartyPrimaryEmails) {
            context.thisUserPrimaryEmail = userLoginPartyPrimaryEmails.get(0)
        }
    }
}
