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
package org.sitenetsoft.sunseterp.applications.order.entry

import org.sitenetsoft.sunseterp.applications.order.shoppingcart.ShoppingCartEvents

shoppingCart = ShoppingCartEvents.getCartObject(request)
orderType = shoppingCart.getOrderType()
context.orderType = orderType

if (!request.getParameterValues('additionalRoleTypeId')) {
    partyType = request.getParameter('additionalPartyType')
    context.additionalPartyType = partyType

    additionalPartyId = request.getParameter('additionalPartyId')
    context.additionalPartyId = additionalPartyId

    roles = from('PartyRole').where('partyId', additionalPartyId).queryList()
    roleData = []
    roles.each { role ->
        roleData.add(role.getRelatedOne('RoleType', false))
    }
    context.roles = roleData
}
