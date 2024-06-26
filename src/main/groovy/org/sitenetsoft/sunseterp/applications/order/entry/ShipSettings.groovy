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

import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityConditionBuilder
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil
import org.sitenetsoft.sunseterp.applications.party.contact.ContactHelper
import org.sitenetsoft.sunseterp.applications.party.contact.ContactMechWorker

cart = session.getAttribute('shoppingCart')

if (cart) {
    createNewShipGroup = request.getParameter('createNewShipGroup')
    if (createNewShipGroup == 'Y') {
        cart.addShipInfo()
    }

    orderPartyId = cart.getPartyId()
    shipToPartyId = parameters.shipToPartyId
    context.cart = cart
    if (shipToPartyId) {
        context.shipToPartyId = shipToPartyId
    } else {
        context.shipToPartyId = cart.getShipToCustomerPartyId()
    }

    // nuke the event messages
    request.removeAttribute('_EVENT_MESSAGE_')

    if (cart.getOrderType() == 'SALES_ORDER') {
        if ('_NA_' != orderPartyId) {
            orderParty = from('Party').where('partyId', orderPartyId).queryOne()
            if (orderParty) {
                shippingContactMechList = ContactHelper.getContactMech(orderParty, 'SHIPPING_LOCATION', 'POSTAL_ADDRESS', false)
                orderPerson = orderParty.getRelatedOne('Person', false)
                context.orderParty = orderParty
                context.orderPerson = orderPerson
                context.shippingContactMechList = shippingContactMechList
            }
        }
        // Ship to another party
        if (context.shipToPartyId != orderPartyId) {
            shipToParty = from('Party').where('partyId', context.shipToPartyId).queryOne()
            if (shipToParty) {
                shipToPartyShippingContactMechList = ContactHelper.getContactMech(shipToParty, 'SHIPPING_LOCATION', 'POSTAL_ADDRESS', false)
                context.shipToPartyShippingContactMechList = shipToPartyShippingContactMechList
            }
        }
        // suppliers for the drop-ship select box
        suppliers = from('PartyRole').where('roleTypeId', 'SUPPLIER').queryList()
        context.suppliers = suppliers

        //resolve Purchase agreements
        EntityConditionBuilder conditionBuilder = new EntityConditionBuilder()
        EntityCondition agreementCond = conditionBuilder.AND {
            IN(partyIdTo: EntityUtil.getFieldListFromEntityList(suppliers, 'partyId', true))
            EQUALS(agreementTypeId: 'PURCHASE_AGREEMENT')
        }
        context.supplierAgreements = from('Agreement').where(agreementCond).queryList()

        // facilities used to reserve the items per ship group
        productStoreFacilities = from('ProductStoreFacility').where('productStoreId', cart.getProductStoreId()).queryList()
        context.productStoreFacilities = productStoreFacilities
} else {
        // Purchase order
        if ('_NA_' != orderPartyId) {
            orderParty = from('Party').where('partyId', orderPartyId).queryOne()
            if (orderParty) {
                orderPerson = orderParty.getRelatedOne('Person', false)
                context.orderParty = orderParty
                context.orderPerson = orderPerson
            }
        }

        companyId = cart.getBillToCustomerPartyId()
        if (companyId) {
            facilityMaps = []
            facilities = from('Facility').where('ownerPartyId', companyId).cache(true).queryList()

            // if facilites is null then check the PartyRelationship where there is a relationship
            // set for Parent & Child organization. Then also fetch the value of companyId from there.
            if (!facilities) {
                partyRelationship = from('PartyRelationship').where('roleTypeIdFrom': 'PARENT_ORGANIZATION', 'partyIdTo': companyId).queryFirst()
                if (partyRelationship) {
                    companyId = partyRelationship.partyIdFrom
                    facilities = from('Facility').where('ownerPartyId', companyId).cache(true).queryList()
                }
            }
            facilities.each { facility ->
                facilityMap = [:]
                facilityContactMechValueMaps = ContactMechWorker.getFacilityContactMechValueMaps(delegator, facility.facilityId, false, null)
                facilityMap.facilityContactMechList = facilityContactMechValueMaps
                facilityMap.facility = facility
                facilityMaps.add(facilityMap)
            }
            context.facilityMaps = facilityMaps
        }
        // Ship to another party
        if (context.shipToPartyId != orderPartyId) {
            shipToParty = from('Party').where('partyId', context.shipToPartyId).queryOne()
            if (shipToParty) {
                shipToPartyShippingContactMechList = ContactHelper.getContactMech(shipToParty, 'SHIPPING_LOCATION', 'POSTAL_ADDRESS', false)
                context.shipToPartyShippingContactMechList = shipToPartyShippingContactMechList
            }
        }
    }
}
