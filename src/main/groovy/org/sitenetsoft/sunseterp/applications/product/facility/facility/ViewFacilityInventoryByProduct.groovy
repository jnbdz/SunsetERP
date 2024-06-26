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
package org.sitenetsoft.sunseterp.applications.product.facility.facility

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator
import org.sitenetsoft.sunseterp.framework.entity.transaction.TransactionUtil

import java.sql.Timestamp
import org.sitenetsoft.sunseterp.framework.entity.model.DynamicViewEntity
import org.sitenetsoft.sunseterp.framework.entity.model.ModelKeyMap

action = request.getParameter('action')
statusId = request.getParameter('statusId')
searchParameterString = ''
searchParameterString = 'action=Y&facilityId=' + facilityId

offsetQOH = -1
offsetATP = -1
hasOffsetQOH = false
hasOffsetATP = false

rows = [] as ArrayList
int listSize = 0 // The complete size of the list of result (for pagination)

if (action) {
    // ------------------------------
    prodView = new DynamicViewEntity()
    conditionMap = [facilityId: facilityId]

    if (offsetQOHQty) {
        try {
            offsetQOH = Integer.parseInt(offsetQOHQty)
            hasOffsetQOH = true
            searchParameterString = searchParameterString + '&offsetQOHQty=' + offsetQOH
        } catch (NumberFormatException nfe) {
            logError(nfe, 'Caught an exception : ' + nfe)
            request.setAttribute('_ERROR_MESSAGE', 'An entered value seems non-numeric')
        }
    }
    if (offsetATPQty) {
        try {
            offsetATP = Integer.parseInt(offsetATPQty)
            hasOffsetATP = true
            searchParameterString = searchParameterString + '&offsetATPQty=' + offsetATP
        } catch (NumberFormatException nfe) {
            logError(nfe, 'Caught an exception : ' + nfe)
            request.setAttribute('_ERROR_MESSAGE', 'An entered value seems non-numeric')
        }
    }

    prodView.addMemberEntity('PRFA', 'ProductFacility')
    prodView.addAliasAll('PRFA', null, null)

    prodView.addMemberEntity('PROD', 'Product')
    prodView.addViewLink('PROD', 'PRFA', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
    prodView.addAlias('PROD', 'internalName')
    prodView.addAlias('PROD', 'isVirtual')
    prodView.addAlias('PROD', 'salesDiscontinuationDate')
    if (productTypeId) {
        prodView.addAlias('PROD', 'productTypeId')
        conditionMap.productTypeId = productTypeId
        searchParameterString = searchParameterString + '&productTypeId=' + productTypeId
    }
    if (searchInProductCategoryId) {
        prodView.addMemberEntity('PRCA', 'ProductCategoryMember')
        prodView.addViewLink('PRFA', 'PRCA', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
        prodView.addAlias('PRCA', 'productCategoryId')
        conditionMap.productCategoryId = searchInProductCategoryId
        searchParameterString = searchParameterString + '&searchInProductCategoryId=' + searchInProductCategoryId
    }

    if (productSupplierId) {
        prodView.addMemberEntity('SPPR', 'SupplierProduct')
        prodView.addViewLink('PRFA', 'SPPR', Boolean.FALSE, ModelKeyMap.makeKeyMapList('productId'))
        prodView.addAlias('SPPR', 'partyId')
        conditionMap.partyId = productSupplierId
        searchParameterString = searchParameterString + '&productSupplierId=' + productSupplierId
    }

    // set distinct on so we only get one row per product
    searchCondition = EntityCondition.makeCondition(conditionMap, EntityOperator.AND)
    notVirtualCondition = EntityCondition.makeCondition(EntityCondition.makeCondition('isVirtual', EntityOperator.EQUALS, null),
                                                         EntityOperator.OR,
                                                         EntityCondition.makeCondition('isVirtual', EntityOperator.NOT_EQUAL, 'Y'))

    whereConditionsList = [searchCondition, notVirtualCondition]
    // add the discontinuation date condition
    if (productsSoldThruTimestamp) {
        discontinuationDateCondition = EntityCondition.makeCondition(
               [
                EntityCondition.makeCondition('salesDiscontinuationDate', EntityOperator.EQUALS, null),
                EntityCondition.makeCondition('salesDiscontinuationDate', EntityOperator.GREATER_THAN, productsSoldThruTimestamp)
               ],
               EntityOperator.OR)
        whereConditionsList.add(discontinuationDateCondition)
        searchParameterString = searchParameterString + '&productsSoldThruTimestamp=' + productsSoldThruTimestamp
    }

    // add search on internal name
    if (internalName) {
        whereConditionsList.add(EntityCondition.makeCondition('internalName', EntityOperator.LIKE, '%' + internalName + '%'))
        searchParameterString = searchParameterString + '&internalName=' + internalName
    }

    // add search on productId
    if (productId) {
        whereConditionsList.add(EntityCondition.makeCondition('productId', EntityOperator.LIKE, productId + '%'))
        searchParameterString = searchParameterString + '&productId=' + productId
    }
    // add statusId in search parametters
    if (statusId) {
        searchParameterString = searchParameterString + '&statusId=' + statusId
    }

    whereCondition = EntityCondition.makeCondition(whereConditionsList, EntityOperator.AND)

    beganTransaction = false
    // get the indexes for the partial list
    lowIndex = ((viewIndex.intValue() * viewSize.intValue()) + 1)
    highIndex = (viewIndex.intValue() + 1) * viewSize.intValue()
    // add viewSize and viewIndex in search parameters
    searchParameterString = searchParameterString + '&VIEW_SIZE=' + viewSize + '&VIEW_INDEX=' + viewIndex
    List prods = null
    try {
        beganTransaction = TransactionUtil.begin()
        prodsEli = from(prodView).where(whereCondition).orderBy('productId').cursorScrollInsensitive().distinct().queryIterator()
        prods = prodsEli.getPartialList(lowIndex, highIndex)
        listSize = prodsEli.getResultsSizeAfterPartialList()
        prodsEli.close()
    } catch (GenericEntityException e) {
        errMsg = 'Failure in operation, rolling back transaction'
        logError(e, errMsg)
        try {
            // only rollback the transaction if we started one...
            TransactionUtil.rollback(beganTransaction, errMsg, e)
        } catch (GenericEntityException e2) {
            logError(e2, 'Could not rollback transaction: ' + e2)
        }
        // after rolling back, rethrow the exception
        throw e
    } finally {
        // only commit the transaction if we started one... this will throw an exception if it fails
        TransactionUtil.commit(beganTransaction)
    }

    // If the user has specified a number of months over which to sum usage quantities, define the correct timestamp
    Timestamp checkTime = null
    monthsInPastLimitStr = request.getParameter('monthsInPastLimit')
    if (monthsInPastLimitStr) {
        try {
            monthsInPastLimit = Integer.parseInt(monthsInPastLimitStr)
            cal = UtilDateTime.toCalendar(null)
            cal.add(Calendar.MONTH, 0 - monthsInPastLimit)
            checkTime = UtilDateTime.toTimestamp(cal.getTime())
            searchParameterString += '&monthsInPastLimit=' + monthsInPastLimitStr
        } catch (Exception e) {
            logError(e, 'Caught an exception : ' + e)
            request.setAttribute('_ERROR_MESSAGE', 'An exception occured please check the log')
        }
    }

    prods.each { oneProd ->
        oneInventory = [:]
        resultMap = [:]
        oneInventory.checkTime = checkTime
        oneInventory.facilityId = facilityId
        oneInventory.productId = oneProd.productId
        minimumStock = oneProd.minimumStock
        oneInventory.minimumStock = minimumStock
        oneInventory.reorderQuantity = oneProd.reorderQuantity
        oneInventory.daysToShip = oneProd.daysToShip

        resultMap = runService('getProductInventoryAndFacilitySummary',
                [productId: oneProd.productId, minimumStock: minimumStock, facilityId: oneProd.facilityId, checkTime: checkTime, statusId: statusId])
        if (resultMap) {
            oneInventory << [
                    totalAvailableToPromise: resultMap.totalAvailableToPromise,
                    totalQuantityOnHand: resultMap.totalQuantityOnHand,
                    quantityOnOrder: resultMap.quantityOnOrder,
                    offsetQOHQtyAvailable: resultMap.offsetQOHQtyAvailable,
                    offsetATPQtyAvailable: resultMap.offsetATPQtyAvailable,
                    quantityUom: resultMap.quantityUomId,
                    usageQuantity: resultMap.usageQuantity,
                    defaultPrice: resultMap.defaultPrice,
                    listPrice: resultMap.listPrice,
                    wholeSalePrice: resultMap.wholeSalePrice
            ]
            if (offsetQOHQty && offsetATPQty) {
                if ((offsetQOHQty && resultMap.offsetQOHQtyAvailable < offsetQOH) && (offsetATPQty && resultMap.offsetATPQtyAvailable < offsetATP)) {
                    rows.add(oneInventory)
                }
            } else if (offsetQOHQty || offsetATPQty) {
                if ((offsetQOHQty && resultMap.offsetQOHQtyAvailable < offsetQOH) || (offsetATPQty && resultMap.offsetATPQtyAvailable < offsetATP)) {
                    rows.add(oneInventory)
                }
            } else {
                rows.add(oneInventory)
            }
        }
    }
}

context.overrideListSize = listSize
context.inventoryByProduct = rows
context.searchParameterString = searchParameterString
