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
package org.sitenetsoft.sunseterp.applications.product.product.cost

import java.math.RoundingMode

import org.sitenetsoft.sunseterp.framework.base.util.UtilDateTime
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties
import org.sitenetsoft.sunseterp.framework.entity.GenericValue
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil

/**
 * Cancels CostComponents
 */
Map cancelCostComponents() {
    Map costsAndMap = [:]
    if (parameters.costComponentId) {
        costsAndMap.costComponentId = parameters.costComponentId
    }
    if (parameters.productId) {
        costsAndMap.productId = parameters.productId
    }
    if (parameters.costUomId) {
        costsAndMap.costUomId = parameters.costUomId
    }
    if (parameters.costComponentTypeId) {
        costsAndMap.costComponentTypeId = parameters.costComponentTypeId
    }
    List existingCosts = from('CostComponent').where(costsAndMap).filterByDate().queryList()
    for (GenericValue existingCost : existingCosts) {
        existingCost.thruDate = UtilDateTime.nowTimestamp()
        existingCost.store()
    }
    return success()
}

/**
 * Create a CostComponent and cancel the existing ones
 */
Map recreateCostComponent() {
    Map result = success()
    // The existing costs of the same type are expired
    Map costsAndMap = [:]
    if (parameters.productId) {
        costsAndMap.productId = parameters.productId
    }
    if (parameters.costUomId) {
        costsAndMap.costUomId = parameters.costUomId
    }
    if (parameters.costComponentTypeId) {
        costsAndMap.costComponentTypeId = parameters.costComponentTypeId
    }
    List existingCosts = from('CostComponent').where(costsAndMap).filterByDate().queryList()
    for (GenericValue existingCost : existingCosts) {
        existingCost.thruDate = UtilDateTime.nowTimestamp()
        existingCost.store()
    }
    // The new cost is created
    GenericValue newEntity = makeValue('CostComponent')
    newEntity.setNonPKFields(parameters)
    newEntity.costComponentId = delegator.getNextSeqId('CostComponent')
    newEntity.fromDate = newEntity.fromDate ?: UtilDateTime.nowTimestamp()
    newEntity.create()
    result.costComponentId = newEntity.costComponentId
    return result
}

// Services to get the product and tasks costs

/**
 * Gets the product's costs (from CostComponent or ProductPrice)
 */
Map getProductCost() {
    Map result = success()
    Map inputMap
    String inputString = "${parameters.costComponentTypePrefix}_%"
    EntityCondition condition = EntityCondition.makeCondition(
            EntityCondition.makeCondition('productId', parameters.productId),
            EntityCondition.makeCondition('costUomId', parameters.currencyUomId),
            EntityCondition.makeCondition('costComponentTypeId', EntityOperator.LIKE , inputString)
            )
    List costComponents = from('CostComponent').where(condition).filterByDate().queryList()
    BigDecimal productCost = (BigDecimal) 0
    for (GenericValue costComponent : costComponents) {
        productCost += costComponent.cost
        // set field="productCost" value="${costComponent.cost + productCost}" type="BigDecimal"/
    }
    productCost = productCost.setScale(6)
    // if the cost is zero, and the product is a variant, get the cost of the virtual
    if (productCost == (BigDecimal) 0) {
        GenericValue product = from('Product').where(parameters).queryOne()
        Map assocAndMap = [productIdTo: product.productId, productAssocTypeId: 'PRODUCT_VARIANT']
        GenericValue virtualAssoc = from('ProductAssoc').where(assocAndMap).filterByDate().queryFirst()
        if (virtualAssoc) {
            inputMap = [productId: virtualAssoc.productId, currencyUomId: parameters.currencyUomId,
                        costComponentTypePrefix: parameters.costComponentTypePrefix]
            Map serviceResult = run service: 'getProductCost', with: inputMap
            productCost = serviceResult.productCost
        }
    }
    // if the cost is zero, get the purchase cost from the SupplierProduct
    if (productCost == (BigDecimal) 0) {
        List orderByList = [
            '+supplierPrefOrderId',
            '+lastPrice'
        ]
        Map costsAndMap = [productId: parameters.productId, currencyUomId: parameters.currencyUomId]
        List priceCosts = from('SupplierProduct').where(costsAndMap).orderBy(orderByList).queryList()
        priceCosts = EntityUtil.filterByDate(priceCosts, UtilDateTime.nowTimestamp(), 'availableFromDate', 'availableThruDate', true)
        if (priceCosts) {
            GenericValue priceCost = priceCosts.get(0)
            if (priceCost.lastPrice) {
                productCost = priceCost.lastPrice
            }
        }
        // if the cost is zero, get the purchase cost from the SupplierProduct in a different currency and try to convert
        if (productCost == (BigDecimal) 0) {
            costsAndMap = [productId: parameters.productId]
            priceCosts = from('SupplierProduct').where(costsAndMap).orderBy(orderByList).queryList()
            priceCosts = EntityUtil.filterByDate(priceCosts, UtilDateTime.nowTimestamp(), 'availableFromDate', 'availableThruDate', true)
            if (priceCosts) {
                GenericValue priceCost = priceCosts.get(0)
                if (priceCost.lastPrice) {
                    // we try to convert the lastPrice to the desired currency
                    inputMap = [originalValue: priceCost.lastPrice, uomId: priceCost.currencyUomId, uomIdTo: parameters.currencyUomId]
                    Map serviceResultCU
                    try {
                        serviceResultCU = dispatcher.runSync('convertUom', inputMap, 7200, true)
                    } catch (Exception e) {
                        serviceResultCU = ServiceUtil.returnError(e.toString())
                    }
                    productCost = serviceResultCU.convertedValue
                    // if currency conversion fails then a 0 cost will be returned
                    if (!productCost) {
                        logWarning('Currency conversion failed for ProductCost lookup;' +
                                " unable to convert from ${priceCost.currencyUomId} to ${parameters.currencyUomId}")
                        productCost = (BigDecimal) 0
                    }
                }
            }
        }
    }
    //    <if-compare field="productCost" operator="equals" value="0" type="BigDecimal">
    //        <clear-field field="costsAndMap"/>
    //        <set from-field="parameters.productId" field="costsAndMap.productId"/>
    //        <set from-field="parameters.currencyUomId" field="costsAndMap.currencyUomId"/>
    //        <set from-field="parameters.productPriceTypeId" field="costsAndMap.productPriceTypeId"/>
    //        <find-by-and entity-name="ProductPrice" map="costsAndMap" list="priceCosts"/>
    //        <filter-list-by-date list="priceCosts"/>
    //        <first-from-list list="priceCosts" entry="priceCost"/>
    //        <if-not-empty field="priceCost.price">
    //            <set from-field="priceCost.price" field="productCost"/>
    //        </if-not-empty>
    //    </if-compare>
    result.productCost = productCost
    return result
}

/**
 * Gets the production run task's costs
 */
Map getTaskCost() {
    Map result = success()
    Map costsByType = [:]
    GenericValue setupCost
    GenericValue usageCost
    // First of all, the estimated task time is computed
    Map inputMap = parameters
    inputMap.taskId = parameters.workEffortId
    Map serviceResult = run service: 'getEstimatedTaskTime', with: inputMap
    Long totalEstimatedTaskTime = serviceResult.estimatedTaskTime
    BigDecimal setupTime = serviceResult.setupTime
    BigDecimal estimatedTaskTime = totalEstimatedTaskTime - setupTime
    estimatedTaskTime = estimatedTaskTime.setScale(6)

    GenericValue task = from('WorkEffort').where(parameters).queryOne()
    if (task) {
        GenericValue fixedAsset = delegator.getRelatedOne('FixedAsset', task, false)
        Map costsAndMap = [amountUomId: parameters.currencyUomId, fixedAssetStdCostTypeId: 'SETUP_COST']
        List setupCosts = delegator.getRelated('FixedAssetStdCost', costsAndMap, null, fixedAsset, false)
        setupCosts = EntityUtil.filterByDate(setupCosts)
        // <filter-list-by-and list-name="costs" map-name="costsAndMap"/>
        setupCost = setupCosts.get(0)
        costsAndMap.fixedAssetStdCostTypeId = 'USAGE_COST'
        List usageCosts = delegator.getRelated('FixedAssetStdCost', costsAndMap, null, fixedAsset, false)
        usageCosts = EntityUtil.filterByDate(usageCosts)
        usageCost = usageCosts.get(0)
    }
    BigDecimal taskCost = (estimatedTaskTime * (usageCost ? usageCost.amount : 0)) + (setupTime * (setupCost ? setupCost.amount : 0))
    taskCost = taskCost.setScale(6)

    // Time is converted from milliseconds to hours
    taskCost /= 3600000
    taskCost = taskCost.setScale(6)

    // Now compute the costs derived from CostComponentCalc records associated with the task
    List weccs = delegator.getRelated('WorkEffortCostCalc', null, null, task, false)
    weccs = EntityUtil.filterByDate(weccs)
    for (GenericValue wecc : weccs) {
        GenericValue costComponentCalc = delegator.getRelatedOne('CostComponentCalc', wecc, false)
        GenericValue customMethod = delegator.getRelatedOne('CustomMethod', costComponentCalc, false)
        if (!customMethod) { // FIXME: formulas are still not supported for standard costs (else part)
            if (costComponentCalc.perMilliSecond) {
                if (costComponentCalc.perMilliSecond != (BigDecimal) 0) {
                    BigDecimal totalCostComponentTime = totalEstimatedTaskTime / costComponentCalc.perMilliSecond
                    totalCostComponentTime = totalCostComponentTime.setScale(6)
                    BigDecimal totalCostComponentCost = totalCostComponentTime * costComponentCalc.variableCost
                    totalCostComponentCost += costComponentCalc.fixedCost
                    totalCostComponentCost = totalCostComponentCost.setScale(6)
                    costsByType."${wecc.costComponentTypeId}" = totalCostComponentCost
                }
            }
        }
    }
    result.taskCost = taskCost
    result.costsByType = costsByType
    return result
}

// services to automatically generate cost information

/**
 * Calculates estimated costs for all the products
 */
Map calculateAllProductsCosts() {
    // filter-by-date="true"
    List products = from('Product').orderBy('-billOfMaterialLevel').select('productId').queryList()
    Map inMap = [currencyUomId: parameters.currencyUomId, costComponentTypePrefix: parameters.costComponentTypePrefix]
    for (GenericValue product : products) {
        inMap.productId = product.productId
        run service: 'calculateProductCosts', with: inMap
    }
    return success()
}

/**
 * Calculates the product's cost
 */
Map calculateProductCosts() {
    Map result = success()
    Map totalCostsByType = [:]
    BigDecimal totalProductCost = (BigDecimal) 0
    BigDecimal totalTaskCost = (BigDecimal) 0
    BigDecimal totalOtherTaskCost = (BigDecimal) 0
    // the existing costs are expired
    Map cancelMap = [costComponentTypeId: (String) "${parameters.costComponentTypePrefix}_ROUTE_COST",
                     productId: parameters.productId, costUomId: parameters.currencyUomId]
    run service: 'cancelCostComponents', with: cancelMap
    cancelMap.costComponentTypeId = (String) "${parameters.costComponentTypePrefix}_MAT_COST"
    run service: 'cancelCostComponents', with: cancelMap
    // calculate the total materials' cost
    Map callSvcMap = [productId: parameters.productId]
    Map serviceResult = run service: 'getManufacturingComponents', with: callSvcMap
    List componentsMap = serviceResult.componentsMap
    if (componentsMap) {
        for (Map componentMap : componentsMap) {
            GenericValue product = componentMap.product
            Map inputMap = [productId: product.productId, currencyUomId: parameters.currencyUomId,
                            costComponentTypePrefix: parameters.costComponentTypePrefix]
            Map serviceResultGPC = run service: 'getProductCost', with: inputMap
            BigDecimal productCost = serviceResultGPC.productCost
            totalProductCost += componentMap.quantity * productCost
            totalProductCost = totalProductCost.setScale(6)
        }
    } else {
        Map inputMap = [productId: parameters.productId, currencyUomId: parameters.currencyUomId,
                        costComponentTypePrefix: parameters.costComponentTypePrefix]
        Map serviceResultGPC = run service: 'getProductCost', with: inputMap
        BigDecimal productCost = serviceResultGPC.productCost
        totalProductCost += productCost
        totalProductCost = totalProductCost.setScale(6)
    }
    // calculate the total tasks' cost
    callSvcMap.ignoreDefaultRouting = 'Y'
    Map serviceResultGPR = run service: 'getProductRouting', with: callSvcMap
    List tasks = serviceResultGPR.tasks
    GenericValue routing = serviceResultGPR.routing
    for (GenericValue task : tasks) {
        callSvcMap = [workEffortId: task.workEffortIdTo, currencyUomId: parameters.currencyUomId,
                      productId: parameters.productId, routingId: routing.workEffortId]
        Map serviceResultGTC = run service: 'getTaskCost', with: callSvcMap
        BigDecimal taskCost = serviceResultGTC.taskCost
        Map costsByType = serviceResultGTC.costsByType
        totalTaskCost += taskCost
        totalTaskCost = totalTaskCost.setScale(6)
        costsByType.entrySet().each { Map.Entry entry ->
            if (totalCostsByType."${entry.key}") {
                totalCostsByType."${entry.key}" = entry.value + totalCostByType."${entry.key}"
            } else {
                totalCostsByType."${entry.key}" = entry.value
            }
            totalOtherTaskCost += entry.value
            totalOtherTaskCost = totalOtherTaskCost.setScale(6)
            totalCostsByType."${entry.key}" = (totalCostsByType."${entry.key}").setScale(6)
        }
    }
    BigDecimal totalCost = totalTaskCost + totalProductCost + totalOtherTaskCost
    totalCost = totalCost.setScale(6)

    // The CostComponent records are created.
    if (totalTaskCost > (BigDecimal) 0) {
        callSvcMap = [costComponentTypeId: (String) "${parameters.costComponentTypePrefix}_ROUTE_COST",
                      productId: parameters.productId, costUomId: parameters.currencyUomId, cost: totalTaskCost]
        run service: 'recreateCostComponent', with: callSvcMap
    }
    if (totalProductCost > (BigDecimal) 0) {
        callSvcMap = [costComponentTypeId: (String) "${parameters.costComponentTypePrefix}_MAT_COST", productId: parameters.productId,
                      costUomId: parameters.currencyUomId, cost: totalProductCost]
        run service: 'recreateCostComponent', with: callSvcMap
    }
    for (Map.Entry entry : totalCostsByType.entrySet()) {
        String costType = entry.getKey()
        BigDecimal totalCostAmount = entry.getValue()
        callSvcMap = [costComponentTypeId: "${parameters.costComponentTypePrefix}_${costType}", productId: parameters.productId,
                      costUomId: parameters.currencyUomId, cost: totalCostAmount]
        run service: 'recreateCostComponent', with: callSvcMap
    }
    // Now compute the costs derived from CostComponentCalc records associated with the product
    List productCostComponentCalcs = from('ProductCostComponentCalc')
            .where(productId: parameters.productId).filterByDate().orderBy('sequenceNum').queryList()
    for (GenericValue productCostComponentCalc : productCostComponentCalcs) {
        GenericValue costComponentCalc = delegator.getRelatedOne('CostComponentCalc', productCostComponentCalc, false)
        GenericValue customMethod = delegator.getRelatedOne('CustomMethod', costComponentCalc, false)
        if (customMethod) {
            Map customMethodParameters = [productCostComponentCalc: productCostComponentCalc, costComponentCalc: costComponentCalc,
                                          currencyUomId: parameters.currencyUomId, costComponentTypePrefix: parameters.costComponentTypePrefix,
                                          baseCost: totalCost]
            Map serviceResultCM = run service: "${customMethod.customMethodName}", with: customMethodParameters
            BigDecimal productCostAdjustment = serviceResultCM.productCostAdjustment
            callSvcMap = [costComponentTypeId: (String) "${parameters.costComponentTypePrefix}_${productCostComponentCalc.costComponentTypeId}",
                          productId: productCostComponentCalc.productId, costUomId: parameters.currencyUomId, cost: productCostAdjustment]
            run service: 'recreateCostComponent', with: callSvcMap
            // set field="totalCost" value="${totalCost + productCostAdjustment}" type="BigDecimal"/
            totalCost += productCostAdjustment
            totalCost = totalCost.setScale(6)
        } else {
            // TODO: not supported for CostComponentCalc entries directly associated to a product
            logWarning("Unable to create cost component for cost component calc with id [${costComponentCalc.costComponentCalcId}]" +
                    ' because customMethod is not set')
        }
    }
    result.totalCost = totalCost
    return result
}

/**
 * Calculate inventory average cost for a product
 */
Map calculateProductAverageCost() {
    Map result = success()
    EntityCondition condition = EntityCondition.makeCondition(
            EntityCondition.makeCondition('productId', parameters.productId),
            EntityCondition.makeCondition('unitCost', EntityOperator.NOT_EQUAL, null)
            )
    if (parameters.facilityId) {
        condition = EntityCondition.makeCondition(condition, EntityCondition.makeCondition('facilityId', parameters.facilityId))
    }
    if (parameters.ownerPartyId) {
        condition = EntityCondition.makeCondition(condition, EntityCondition.makeCondition('ownerPartyId', parameters.ownerPartyId))
    }

    List inventoryItems = from('InventoryItem').where(condition).select('quantityOnHandTotal', 'unitCost', 'currencyUomId').queryList()
    BigDecimal totalQuantityOnHand = (BigDecimal) 0
    BigDecimal totalInventoryCost = (BigDecimal) 0
    BigDecimal absValOfTotalQOH = (BigDecimal) 0
    BigDecimal absValOfTotalInvCost = (BigDecimal) 0
    Boolean differentCurrencies = false
    String currencyUomId
    for (GenericValue inventoryItem : inventoryItems) {
        totalQuantityOnHand += inventoryItem.quantityOnHandTotal
        currencyUomId = currencyUomId ?: inventoryItem.currencyUomId
        if (!differentCurrencies) {
            if (currencyUomId == inventoryItem.currencyUomId) {
                totalInventoryCost += (inventoryItem.unitCost * inventoryItem.quantityOnHandTotal)

                // calculation of absolute values of QOH and total inventory cost
                if (inventoryItem.quantityOnHandTotal < (BigDecimal) 0) {
                    absValOfTotalQOH = absValOfTotalQOH - inventoryItem.quantityOnHandTotal
                    absValOfTotalInvCost = absValOfTotalInvCost + (-1 * inventoryItem.quantityOnHandTotal * inventoryItem.unitCost)
                } else {
                    absValOfTotalQOH += inventoryItem.quantityOnHandTotal
                    absValOfTotalInvCost = absValOfTotalInvCost + (inventoryItem.quantityOnHandTotal * inventoryItem.unitCost)
                }
            } else {
                differentCurrencies = true
            }
        }
    }
    BigDecimal productAverageCost = (BigDecimal) 0
    if (absValOfTotalQOH != (BigDecimal) 0) {
        productAverageCost = absValOfTotalInvCost / absValOfTotalQOH
    }
    result.totalQuantityOnHand = totalQuantityOnHand.setScale(2, RoundingMode.HALF_EVEN)
    if (!differentCurrencies) {
        result.totalInventoryCost = totalInventoryCost.setScale(2, RoundingMode.HALF_EVEN)
        result.productAverageCost = productAverageCost.setScale(2, RoundingMode.HALF_EVEN)
        result.currencyUomId = currencyUomId
    }
    return result
}

/**
 * Update a Product Average Cost record on receive inventory
 */
Map updateProductAverageCostOnReceiveInventory() {
    GenericValue inventoryItem = from('InventoryItem').where(parameters).queryOne()
    String organizationPartyId = inventoryItem?.ownerPartyId
    if (!organizationPartyId) {
        GenericValue facility = from('Facility').where(parameters).queryOne()
        organizationPartyId = facility?.ownerPartyId
        if (!organizationPartyId) {
            GenericValue productStore = delegator.getRelatedOne('ProductStore', facility, false)
            organizationPartyId = productStore?.ownerPartyId
            if (!organizationPartyId) {
                String errorMessage = UtilProperties.getMessage('ProductUiLabels', 'ProductOwnerPartyIsMissing', locale)
                logError(errorMessage)
                return error(errorMessage)
            }
        }
    }
    GenericValue productAverageCost = from('ProductAverageCost').where(productId: parameters.productId, facilityId: parameters.facilityId,
            productAverageCostTypeId: 'SIMPLE_AVG_COST', organizationPartyId: organizationPartyId).filterByDate().queryFirst()
    // <log level="always" message="In updateProductAverageCostOnReceiveInventory found productAverageCost: ${productAverageCost}"/>
    Map productAverageCostMap = parameters
    productAverageCostMap.productAverageCostTypeId = 'SIMPLE_AVG_COST'
    productAverageCostMap.organizationPartyId = organizationPartyId
    Map updateProductAverageCostMap = [:]
    if (productAverageCost) {
        // Expire existing one and calculate average cost
        updateProductAverageCostMap << productAverageCost
        updateProductAverageCostMap.thruDate = UtilDateTime.nowTimestamp()
        run service: 'updateProductAverageCost', with: updateProductAverageCostMap

        Map serviceInMap = [productId: parameters.productId, facilityId: parameters.facilityId]
        Map serviceResultGIABF = run service: 'getInventoryAvailableByFacility', with: serviceInMap
        BigDecimal quantityOnHandTotal = serviceResultGIABF.quantityOnHandTotal
        BigDecimal oldProductQuantity = quantityOnHandTotal - parameters.quantityAccepted
        BigDecimal averageCost = ((productAverageCost.averageCost * oldProductQuantity)
                + (inventoryItem.unitCost * parameters.quantityAccepted)) / (quantityOnHandTotal)
        int roundingDecimal = UtilProperties.getPropertyAsInteger('arithmetic', 'finaccout.decimals', 2)
        String roundingMode = UtilProperties.getPropertyValue('arithmetic', 'finaccount.roundingGroovyMethod', 'HALF_UP')
        averageCost = averageCost.setScale(roundingDecimal, RoundingMode."${roundingMode}")
        productAverageCostMap.averageCost = averageCost
        productAverageCostMap.fromDate = UtilDateTime.nowTimestamp()
    } else {
        productAverageCostMap.averageCost = inventoryItem.unitCost
    }
    run service: 'createProductAverageCost', with: productAverageCostMap
    logInfo("For facilityId ${parameters.facilityId}, Average cost of product ${parameters.productId} " +
            "is set from  ${updateProductAverageCostMap.averageCost} to ${productAverageCostMap.averageCost}")
    return success()
}

// Service to get the average cost of product
Map getProductAverageCost() {
    Map result = success()
    GenericValue productAverageCost
    BigDecimal unitCost
    GenericValue inventoryItem = parameters.inventoryItem
    Map getPartyAcctgPrefMap = [organizationPartyId: inventoryItem.ownerPartyId]
    Map serviceResult = run service: 'getPartyAccountingPreferences', with: getPartyAcctgPrefMap
    GenericValue partyAccountingPreference = serviceResult.partyAccountingPreference
    if (partyAccountingPreference.cogsMethodId == 'COGS_AVG_COST') {
        // TODO: handle productAverageCostTypeId for WEIGHTED_AVG_COST and MOVING_AVG_COST
        productAverageCost = from('ProductAverageCost')
                .where(productAverageCostTypeId: 'SIMPLE_AVG_COST', organizationPartyId: inventoryItem.ownerPartyId,
                        productId: inventoryItem.productId, facilityId: inventoryItem.facilityId)
                .filterByDate()
                .queryFirst()
    }
    if (productAverageCost) {
        unitCost = productAverageCost.averageCost
    } else {
        unitCost = inventoryItem.unitCost
    }
    result.unitCost = unitCost
    return result
}

/**
 * Formula that creates a cost component equal to a percentage of total product cost
 */
Map productCostPercentageFormula() {
    Map result = success()
    GenericValue productCostComponentCalc = parameters.productCostComponentCalc
    GenericValue costComponentCalc = parameters.costComponentCalc
    Map inputMap = [productId: productCostComponentCalc.productId, currencyUomId: parameters.currencyUomId,
                    costComponentTypePrefix: parameters.costComponentTypePrefix]
    run service: 'getProductCost', with: inputMap
    // set field="productCostAdjustment" value="${parameters.baseCost * costComponentCalc.fixedCost}" type="BigDecimal"/
    BigDecimal productCostAdjustment = costComponentCalc.fixedCost * parameters.baseCost
    productCostAdjustment = productCostAdjustment.setScale(6)
    result.productCostAdjustment = productCostAdjustment
    return result
}
