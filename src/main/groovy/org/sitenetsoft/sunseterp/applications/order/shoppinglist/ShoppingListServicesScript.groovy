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
package org.sitenetsoft.sunseterp.applications.order.shoppinglist

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties
import org.sitenetsoft.sunseterp.framework.entity.GenericValue
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil

/**
 * Create a ShoppingList
 */
Map createShoppingList() {
    GenericValue newEntity = makeValue('ShoppingList')
    newEntity.setNonPKFields(parameters)
    newEntity.partyId = newEntity.partyId ?: userLogin.partyId
    newEntity.shoppingListTypeId = newEntity.shoppingListTypeId ?: 'SLT_WISH_LIST'
    newEntity.isPublic = newEntity.isPublic ?: 'N'

    newEntity.listName = newEntity.listName ?: UtilProperties.getMessage('OrderUiLabels',
            'OrderNewShoppingList', parameters.locale) ?: 'New Shopping List'

    newEntity.isActive = newEntity.isActive ?: newEntity.shoppingListTypeId == 'SLT_AUTO_REODR' ? 'N' : 'Y'

    newEntity.shoppingListId = delegator.getNextSeqId('ShoppingList')
    newEntity.create()

    Map result = success()
    result.shoppingListId = newEntity.shoppingListId
    result.successMessage = UtilProperties.getMessage('OrderUiLabels', 'OrderShoppingListCreatedSuccessfully', parameters.locale)
    return result
}

/**
 * Update a ShoppingList
 */
Map updateShoppingList() {
    GenericValue shoppingList = from('ShoppingList').where(parameters).queryOne()
    shoppingList.setNonPKFields(parameters)

    // don't let auto-reorders be active unless there is some recurrence info
    if (shoppingList.shoppingListTypeId == 'SLT_AUTO_REODR' &&
            (!shoppingList.recurrenceInfoId ||
                    !shoppingList.paymentMethodId ||
                    !shoppingList.contactMechId ||
                    !shoppingList.shipmentMethodTypeId)) {
        shoppingList.isActive =  'N'
    }
    shoppingList.store()

    Map result = success()
    result.successMessage = UtilProperties.getMessage('OrderUiLabels', 'OrderShoppingListUpdatedSuccessfully', parameters.locale)
    return result
}

/**
 * Create a ShoppingList Item
 */
Map createShoppingListItem() {
    Map result = success()
    List shoppingListItems = from('ShoppingListItem')
            .where(productId: parameters.productId,
            shoppingListId: parameters.shoppingListId)
            .queryList()
    // Check if we have a matching ShoppingListItem (with equal ShoppingListItemAttributes!) and update its quantity
    if (shoppingListItems) {
        for (GenericValue shoppingListItem : shoppingListItems) {
            List slItemAttributes = select('attrName', 'attrValue')
                    .from('ShoppingListItemAttribute')
                    .where('shoppingListId', parameters.shoppingListId, 'shoppingListItemSeqId', shoppingListItem.shoppingListItemSeqId)
                    .queryList()
            if ((!slItemAttributes && !parameters.shoppingListItemAttributes) ||
                    Objects.equals(slItemAttributes, parameters.shoppingListItemAttributes)) {
                BigDecimal totalquantity = shoppingListItem.quantity + parameters.quantity
                result.shoppingListItemSeqId = shoppingListItem.shoppingListItemSeqId
                Map serviceResult = run service: 'updateShoppingListItem', with: [*: shoppingListItem, quantity: totalquantity]
                if (!ServiceUtil.isSuccess(serviceResult)) {
                    return error(serviceResult.errorMessage)
                }
                // Exit here, because we found an existing item update, otherwise we have to create a new one below
                return result
            }
        }
    }
    // Create new ShoppingListItem
    GenericValue shoppingList = from('ShoppingList').where(parameters).queryOne()
    GenericValue product = from('Product').where(parameters).queryOne()
    if (!product) {
        return error(UtilProperties.getMessage('ProductUiLabels', 'ProductErrorProductNotFound', parameters.locale))
    }
    GenericValue newEntity = makeValue('ShoppingListItem')
    newEntity.setNonPKFields(parameters)
    newEntity.shoppingListId = parameters.shoppingListId
    delegator.setNextSubSeqId(newEntity, 'shoppingListItemSeqId', 5, 1)
    newEntity.create()

    result.shoppingListItemSeqId = newEntity.shoppingListItemSeqId
    updateLastAdminModified(shoppingList, userLogin)

    return result
}

/**
 * Update a ShoppingListItem
 */
Map updateShoppingListItem() {
    GenericValue shoppingList = from('ShoppingList').where(parameters).queryOne()
    GenericValue shoppingListItem = from('ShoppingListItem').where(parameters).queryOne()
    shoppingListItem.setNonPKFields(parameters)
    shoppingListItem.store()

    updateLastAdminModified(shoppingList, userLogin)
    return success()
}

/**
 * Remove a ShoppingListItem
 */
Map removeShoppingListItem() {
    GenericValue shoppingList = from('ShoppingList').where(parameters).queryOne()
    GenericValue shoppingListItem = from('ShoppingListItem').where(parameters).queryOne()
    shoppingListItem.remove()

    updateLastAdminModified(shoppingList, userLogin)
    return success()
}

/**
 * Adds a shopping list item if one with the same productId does not exist
 */
Map addDistinctShoppingListItem() {
    Map result = success()
    List shoppingListItemList = from('ShoppingListItem').where(shoppingListId: parameters.shoppingListId).queryList()

    for (GenericValue shoppingListItem : shoppingListItemList) {
        if (parameters.productId == shoppingListItem.productId) {
            result.shoppingListItemSeqId = shoppingListItem.shoppingListItemSeqId
            return result
        }
    }
    Map serviceResult = run service: 'createShoppingListItem', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return error(serviceResult.errorMessage)
    }

    result.shoppingListItemSeqId = serviceResult.shoppingListItemSeqId
    return result
}

/**
 * Calculate Deep Total Price for a ShoppingList
 */
Map calculateShoppingListDeepTotalPrice() {
    Map result = success()
    Map serviceResult = run service: 'checkShoppingListItemSecurity', with: parameters
    if (!ServiceUtil.isSuccess(serviceResult)) {
        return error(serviceResult.errorMessage)
    }
    if (!serviceResult.hasPermission) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunForAnotherParty', parameters.locale))
    }
    Map calcPriceInBaseMap = [prodCatalogId: parameters.prodCatalogId, webSiteId: parameters.webSiteId]
    ['partyId', 'productStoreId', 'productStoreGroupId', 'currencyUomId', 'autoUserLogin'].each {
        if (parameters[it]) {
            calcPriceInBaseMap[it] = parameters[it]
        }
    }

    BigDecimal totalPrice = 0.0
    from('ShoppingListItem')
        .where(shoppingListId: parameters.shoppingListId)
        .cache()
        .queryList()
        .each {
            BigDecimal itemPrice = it.modifiedPrice
            if (!itemPrice) {
                GenericValue product = from('Product').where(productId: it.productId).cache().queryOne()
                Map serviceResultCPP = run service: 'calculateProductPrice', with: [*: calcPriceInBaseMap,
                                                                                    product: product,
                                                                                    quantity: it.quantity]
                if (!ServiceUtil.isSuccess(serviceResultCPP)) {
                    return error(serviceResultCPP.errorMessage)
                }
                itemPrice = serviceResultCPP.price
            }
            BigDecimal shoppingListItemQuantity = it.quantity ?: 1.0
            totalPrice += (itemPrice * shoppingListItemQuantity)
        }

    from('ShoppingList')
        .where(parentShoppingListId: parameters.shoppingListId,
                partyId: userLogin.partyId)
        .cache()
        .queryList()
        .each {
            Map serviceResultCSLDTP = run service: 'calculateShoppingListDeepTotalPrice', with: [*             : calcPriceInBaseMap,
                                                                                                 shoppingListId: it.shoppingListId]
            if (!ServiceUtil.isSuccess(serviceResultCSLDTP)) {
                return error(serviceResultCSLDTP.errorMessage)
            }
            totalPrice += serviceResultCSLDTP.totalPrice
        }

    result.totalPrice = totalPrice
    return result
}

/**
 * Checks security on a ShoppingList
 */
Map checkShoppingListSecurity() {
    if (userLogin && (userLogin.userLoginId != 'anonymous') &&
            parameters.partyId && (userLogin.partyId != parameters.partyId)
            && !security.hasEntityPermission('PARTYMGR', "_${parameters.permissionAction}", parameters.userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels', 'OrderSecurityErrorToRunForAnotherParty', parameters.locale))
    }

    Map result = success()
    result.hasPermission = true
    return result
}

/**
 * Checks security on a ShoppingListIte
 */
Map checkShoppingListItemSecurity() {
    GenericValue shoppingList = from('ShoppingList').where(parameters).queryOne()
    if (shoppingList?.partyId && userLogin.partyId != shoppingList.partyId &&
            !security.hasEntityPermission('PARTYMGR', "_${parameters.permissionAction}", parameters.userLogin)) {
        return error(UtilProperties.getMessage('OrderErrorUiLabels',
                'OrderSecurityErrorToRunForAnotherParty',
                [parentMethodName: parameters.parentMethodName,
                 permissionAction: parameters.permissionAction],
                parameters.locale))
    }

    Map result = success()
    result.hasPermission = true
    return result
}

/**
 * Add suggestions to a shopping list
 */
Map addSuggestionsToShoppingList() {
    Map result = success()
    String shoppingListId
    // first check the ProductStore.enableAutoSuggestionList indicator
    GenericValue orderHeader = from('OrderHeader').where(parameters).queryOne()
    if (!(orderHeader?.productStoreId)) {
        return result
    }
    GenericValue productStore = from('ProductStore').where(productStoreId: orderHeader.productStoreId).cache().queryOne()
    if (productStore.enableAutoSuggestionList != 'Y') {
        return result
    }

    GenericValue orderRole = from ('OrderRole').where(orderId: parameters.orderId, roleTypeId: 'PLACING_CUSTOMER').queryFirst()
    GenericValue shoppingList = from('ShoppingList').where(partyId: orderRole.partyId, listName: 'Auto Suggestions').queryFirst()
    if (shoppingList) {
        shoppingListId = shoppingList.shoppingListId
    } else {
        Map createShoppingListInMap = [partyId: orderRole.partyId,
                                       listName: 'Auto Suggestions',
                                       shoppingListTypeId: 'SLT_WISH_LIST',
                                       productStoreId: parameters.productStoreId]
        Map serviceResultCSL = dispatcher.runSync('createShoppingList', createShoppingListInMap, 7200, true)
        if (!ServiceUtil.isSuccess(serviceResultCSL)) {
            return error(serviceResultCSL.errorMessage)
        }
        shoppingListId = serviceResultCSL.serviceResult
    }
    List orderItemList = from ('OrderItem').where(orderId: parameters.orderId).orderBy('orderItemSeqId').queryList()
    for (GenericValue orderItem : orderItemList) {
        if (orderItem.productId) {
            linkProductToShoppingList(orderItem.productId, shoppingListId)
            GenericValue product = from('Product').where(productId: orderItem.productId).cache().queryOne()
            if (product.isVariant == 'Y') {
                GenericValue virtualProductAssoc = from('ProductAssoc')
                        .where(productIdTo: orderItem.productId,
                                productAssocTypeId: 'PRODUCT_VARIANT')
                        .filterByDate()
                        .queryFirst()
                if (virtualProductAssoc) {
                    linkProductToShoppingList(virtualProductAssoc.productIdTo, shoppingListId)
                }
            }
        }
    }
    return result
}

private List<GenericValue> linkProductToShoppingList(String productId, String shoppingListId) {
    from('ProductAssoc')
        .where(productId: productId,
                productAssocTypeId: 'PRODUCT_COMPLEMENT')
        .filterByDate()
        .queryList().each {
                        run service: 'addDistinctShoppingListItem', with: [productId: it.productIdTo,
                                                                           shoppingListId: shoppingListId,
                                                                           quantity: (BigDecimal) 1]
        }
}

private void updateLastAdminModified(GenericValue shoppingList, GenericValue userLogin) {
    if (shoppingList.partyId != userLogin.partyId) {
        shoppingList.lastAdminModified = UtilDateTime.nowTimestamp()
        shoppingList.store()
    }
}
