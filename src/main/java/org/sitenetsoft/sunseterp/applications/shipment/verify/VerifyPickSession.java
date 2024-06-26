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

package org.sitenetsoft.sunseterp.applications.shipment.verify;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.DelegatorFactory;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceContainer;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

/**
 * The type Verify pick session.
 */
@SuppressWarnings("serial")
public class VerifyPickSession implements Serializable {

    private static final String MODULE = VerifyPickSession.class.getName();

    private GenericValue userLogin = null;
    private String dispatcherName = null;
    private String delegatorName = null;
    private String picklistBinId = null;
    private String facilityId = null;
    private List<VerifyPickSessionRow> pickRows = null;

    private transient Delegator delegator = null;
    private transient LocalDispatcher dispatcher = null;

    public VerifyPickSession() {
    }

    public VerifyPickSession(LocalDispatcher dispatcher, GenericValue userLogin) {
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();
        this.delegator = dispatcher.getDelegator();
        this.delegatorName = delegator.getDelegatorName();
        this.userLogin = userLogin;
        this.pickRows = new LinkedList<>();
    }

    /**
     * Gets dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    /**
     * Gets delegator.
     * @return the delegator
     */
    public Delegator getDelegator() {
        if (delegator == null) {
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        return delegator;
    }

    /**
     * Create row.
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId the product id
     * @param originGeoId the origin geo id
     * @param quantity the quantity
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    public void createRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String originGeoId,
                          BigDecimal quantity, Locale locale) throws GeneralException {

        if (orderItemSeqId == null && productId != null) {
            orderItemSeqId = this.findOrderItemSeqId(productId, orderId, shipGroupSeqId, quantity, locale);
        }

        // get the reservations for the item
        Map<String, Object> inventoryLookupMap = new HashMap<>();
        inventoryLookupMap.put("orderId", orderId);
        inventoryLookupMap.put("orderItemSeqId", orderItemSeqId);
        inventoryLookupMap.put("shipGroupSeqId", shipGroupSeqId);
        List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", inventoryLookupMap,
                UtilMisc.toList("quantity DESC"), false);

        // no reservations we cannot add this item
        if (UtilValidate.isEmpty(reservations)) {
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels",
                    "ProductErrorNoInventoryReservationsAvailableCannotVerifyThisItem", locale));
        }

        if (reservations.size() == 1) {
            GenericValue reservation = EntityUtil.getFirst(reservations);
            int checkCode = this.checkRowForAdd(reservation, orderId, orderItemSeqId, shipGroupSeqId, productId, quantity);
            this.createVerifyPickRow(checkCode, reservation, orderId, orderItemSeqId, shipGroupSeqId, productId, originGeoId, quantity, locale);
        } else {
            // more than one reservation found
            Map<GenericValue, BigDecimal> reserveQtyMap = new HashMap<>();
            BigDecimal qtyRemain = quantity;

            for (GenericValue reservation : reservations) {
                if (qtyRemain.compareTo(BigDecimal.ZERO) > 0) {
                    if (!productId.equals(reservation.getRelatedOne("InventoryItem", false).getString("productId"))) {
                        continue;
                    }
                    BigDecimal reservedQty = reservation.getBigDecimal("quantity");
                    BigDecimal resVerifiedQty = this.getVerifiedQuantity(orderId, orderItemSeqId, shipGroupSeqId, productId,
                            reservation.getString("inventoryItemId"));
                    if (resVerifiedQty.compareTo(reservedQty) >= 0) {
                        continue;
                    } else {
                        reservedQty = reservedQty.subtract(resVerifiedQty);
                    }
                    BigDecimal thisQty = reservedQty.compareTo(qtyRemain) > 0 ? qtyRemain : reservedQty;
                    int thisCheck = this.checkRowForAdd(reservation, orderId, orderItemSeqId, shipGroupSeqId, productId, thisQty);
                    switch (thisCheck) {
                    case 2:
                        // new verify pick row will be created
                        reserveQtyMap.put(reservation, thisQty);
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 1:
                        // existing verify pick row has been updated
                        qtyRemain = qtyRemain.subtract(thisQty);
                        break;
                    case 0:
                        //doing nothing
                        break;
                    }
                }
            }
            if (qtyRemain.compareTo(BigDecimal.ZERO) == 0) {
                for (Map.Entry<GenericValue, BigDecimal> entry : reserveQtyMap.entrySet()) {
                    GenericValue reservation = entry.getKey();
                    BigDecimal qty = entry.getValue();
                    this.createVerifyPickRow(2, reservation, orderId, orderItemSeqId, shipGroupSeqId, productId, originGeoId, qty, locale);
                }
            } else {
                throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels",
                        "ProductErrorNotEnoughInventoryReservationAvailableCannotVerifyTheItem", locale));
            }
        }
    }

    /**
     * Find order item seq id string.
     * @param productId the product id
     * @param orderId the order id
     * @param shipGroupSeqId the ship group seq id
     * @param quantity the quantity
     * @param locale the locale
     * @return the string
     * @throws GeneralException the general exception
     */
    protected String findOrderItemSeqId(String productId, String orderId, String shipGroupSeqId, BigDecimal quantity, Locale locale)
            throws GeneralException {

        Map<String, Object> orderItemLookupMap = new HashMap<>();
        orderItemLookupMap.put("orderId", orderId);
        orderItemLookupMap.put("productId", productId);
        orderItemLookupMap.put("statusId", "ITEM_APPROVED");
        orderItemLookupMap.put("shipGroupSeqId", shipGroupSeqId);

        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItemAndShipGroupAssoc", orderItemLookupMap, null, false);

        String orderItemSeqId = null;
        if (orderItems != null) {
            for (GenericValue orderItem : orderItems) {
                // get the reservations for the item
                Map<String, Object> inventoryLookupMap = new HashMap<>();
                inventoryLookupMap.put("orderId", orderId);
                inventoryLookupMap.put("orderItemSeqId", orderItem.getString("orderItemSeqId"));
                inventoryLookupMap.put("shipGroupSeqId", shipGroupSeqId);
                List<GenericValue> reservations = this.getDelegator().findByAnd("OrderItemShipGrpInvRes", inventoryLookupMap, null, false);
                for (GenericValue reservation : reservations) {
                    BigDecimal qty = reservation.getBigDecimal("quantity");
                    if (quantity.compareTo(qty) <= 0) {
                        orderItemSeqId = orderItem.getString("orderItemSeqId");
                        break;
                    }
                }
            }
        }

        if (orderItemSeqId != null) {
            return orderItemSeqId;
        } else {
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels",
                    "ProductErrorNoValidOrderItemFoundForProductWithEnteredQuantity",
                    UtilMisc.toMap("productId", productId, "quantity", quantity), locale));
        }
    }

    /**
     * Check row for add int.
     * @param reservation the reservation
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId the product id
     * @param quantity the quantity
     * @return the int
     */
    protected int checkRowForAdd(GenericValue reservation, String orderId, String orderItemSeqId, String shipGroupSeqId, String productId,
                                 BigDecimal quantity) {
        // check to see if the reservation can hold the requested quantity amount
        String inventoryItemId = reservation.getString("inventoryItemId");
        BigDecimal resQty = reservation.getBigDecimal("quantity");
        VerifyPickSessionRow pickRow = this.getPickRow(orderId, orderItemSeqId, shipGroupSeqId, productId, inventoryItemId);

        if (pickRow == null) {
            if (resQty.compareTo(quantity) < 0) {
                return 0;
            } else {
                return 2;
            }
        } else {
            BigDecimal newQty = pickRow.getReadyToVerifyQty().add(quantity);
            if (resQty.compareTo(newQty) < 0) {
                return 0;
            } else {
                pickRow.setReadyToVerifyQty(newQty);
                return 1;
            }
        }
    }

    /**
     * Create verify pick row.
     * @param checkCode the check code
     * @param res the res
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId the product id
     * @param originGeoId the origin geo id
     * @param quantity the quantity
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    protected void createVerifyPickRow(int checkCode, GenericValue res, String orderId, String orderItemSeqId, String shipGroupSeqId,
                                       String productId, String originGeoId, BigDecimal quantity, Locale locale) throws GeneralException {
        // process the result; add new item if necessary
        switch (checkCode) {
        case 0:
            // not enough reserved
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels",
                    "ProductErrorNotEnoughInventoryReservationAvailableCannotVerifyTheItem", locale));
        case 1:
            // we're all good to go; quantity already updated
            break;
        case 2:
            // need to create a new item
            String inventoryItemId = res.getString("inventoryItemId");
            pickRows.add(new VerifyPickSessionRow(orderId, orderItemSeqId, shipGroupSeqId, productId, originGeoId, inventoryItemId, quantity));
            break;
        default:
            // if a wrong checkCode is given
            Debug.logError("There was a wrong checkCode given in the method createVerifyPickRow: " + checkCode, MODULE);
        }
    }

    /**
     * Gets user login.
     * @return the user login
     */
    public GenericValue getUserLogin() {
        return this.userLogin;
    }

    /**
     * Sets facility id.
     * @param facilityId the facility id
     */
    public void setFacilityId(String facilityId) {
        this.facilityId = facilityId;
    }

    /**
     * Gets facility id.
     * @return the facility id
     */
    public String getFacilityId() {
        return this.facilityId;
    }

    /**
     * Sets picklist bin id.
     * @param setPicklistBinId the set picklist bin id
     */
    public void setPicklistBinId(String setPicklistBinId) {
        this.picklistBinId = setPicklistBinId;
    }

    /**
     * Gets picklist bin id.
     * @return the picklist bin id
     */
    public String getPicklistBinId() {
        return this.picklistBinId;
    }

    /**
     * Gets pick rows.
     * @return the pick rows
     */
    public List<VerifyPickSessionRow> getPickRows() {
        return this.pickRows;
    }

    /**
     * Gets pick rows.
     * @param orderId the order id
     * @return the pick rows
     */
    public List<VerifyPickSessionRow> getPickRows(String orderId) {
        List<VerifyPickSessionRow> pickVerifyRows = new LinkedList<>();
        for (VerifyPickSessionRow line: this.getPickRows()) {
            if (orderId.equals(line.getOrderId())) {
                pickVerifyRows.add(line);
            }
        }
        return pickVerifyRows;
    }

    /**
     * Gets ready to verify quantity.
     * @param orderId the order id
     * @param orderSeqId the order seq id
     * @return the ready to verify quantity
     * @throws GeneralException the general exception
     */
    public BigDecimal getReadyToVerifyQuantity(String orderId, String orderSeqId) throws GeneralException {
        BigDecimal readyToVerifyQty = BigDecimal.ZERO;
        for (VerifyPickSessionRow line: this.getPickRows()) {
            if ((orderId.equals(line.getOrderId())) && (orderSeqId.equals(line.getOrderItemSeqId()))) {
                readyToVerifyQty = readyToVerifyQty.add(line.getReadyToVerifyQty());
            }
        }
        return readyToVerifyQty;
    }

    /**
     * Gets pick row.
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId the product id
     * @param inventoryItemId the inventory item id
     * @return the pick row
     */
    public VerifyPickSessionRow getPickRow(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId) {
        for (VerifyPickSessionRow line : this.getPickRows(orderId)) {
            if (orderItemSeqId.equals(line.getOrderItemSeqId()) && shipGroupSeqId.equals(line.getShipGroupSeqId())
                    && productId.equals(line.getProductId()) && inventoryItemId.equals(line.getInventoryItemId())) {
                return line;
            }
        }
        return null;
    }

    /**
     * Gets verified quantity.
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @param productId the product id
     * @param inventoryItemId the inventory item id
     * @return the verified quantity
     */
    public BigDecimal getVerifiedQuantity(String orderId, String orderItemSeqId, String shipGroupSeqId, String productId, String inventoryItemId) {
        BigDecimal total = BigDecimal.ZERO;
        for (VerifyPickSessionRow pickRow : this.getPickRows(orderId)) {
            if (orderItemSeqId.equals(pickRow.getOrderItemSeqId()) && shipGroupSeqId.equals(pickRow.getShipGroupSeqId())
                    && productId.equals(pickRow.getProductId())) {
                if (inventoryItemId == null || inventoryItemId.equals(pickRow.getInventoryItemId())) {
                    total = total.add(pickRow.getReadyToVerifyQty());
                }
            }
        }
        return total;
    }

    /**
     * Clear all rows.
     */
    public void clearAllRows() {
        this.pickRows.clear();
    }

    /**
     * Complete string.
     * @param orderId the order id
     * @param locale  the locale
     * @return the string
     * @throws GeneralException the general exception
     */
    public String complete(String orderId, Locale locale) throws GeneralException {
        this.checkVerifiedQty(orderId, locale);
        // check reserved quantity, it should be equal to verified quantity
        this.checkReservedQty(orderId, locale);
        String shipmentId = this.createShipment((this.getPickRows(orderId)).get(0));

        this.issueItemsToShipment(shipmentId, locale);
        this.updateProduct();

        // Update the shipment status to Picked, this will trigger createInvoicesFromShipment and finally a invoice will be created
        Map<String, Object> updateShipmentCtx = new HashMap<>();
        updateShipmentCtx.put("shipmentId", shipmentId);
        updateShipmentCtx.put("statusId", "SHIPMENT_PICKED");
        updateShipmentCtx.put("userLogin", this.getUserLogin());
        Map<String, Object> serviceResult = this.getDispatcher().runSync("updateShipment", updateShipmentCtx);
        if (ServiceUtil.isError(serviceResult)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(serviceResult));
        }

        return shipmentId;
    }

    /**
     * Check reserved qty.
     * @param orderId the order id
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    protected void checkReservedQty(String orderId, Locale locale) throws GeneralException {
        List<String> errorList = new LinkedList<>();
        for (VerifyPickSessionRow pickRow : this.getPickRows(orderId)) {
            BigDecimal reservedQty = this.getReservedQty(pickRow.getOrderId(), pickRow.getOrderItemSeqId(), pickRow.getShipGroupSeqId());
            BigDecimal verifiedQty = this.getReadyToVerifyQuantity(pickRow.getOrderId(), pickRow.getOrderItemSeqId());
            if (verifiedQty.compareTo(reservedQty) != 0) {
                errorList.add(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorVerifiedQtyDoesNotMatchTheReservedQtyForItem",
                        UtilMisc.toMap("productId", pickRow.getProductId(), "verifiedQty", pickRow.getReadyToVerifyQty(), "reservedQty",
                                reservedQty), locale));
            }
        }

        if (!errorList.isEmpty()) {
            throw new GeneralException(UtilProperties.getMessage("OrderErrorUiLabels", "OrderErrorAttemptToVerifyOrderFailed",
                    UtilMisc.toMap("orderId", orderId), locale), errorList);
        }
    }

    /**
     * Gets reserved qty.
     * @param orderId the order id
     * @param orderItemSeqId the order item seq id
     * @param shipGroupSeqId the ship group seq id
     * @return the reserved qty
     */
    public BigDecimal getReservedQty(String orderId, String orderItemSeqId, String shipGroupSeqId) {
        BigDecimal reservedQty = BigDecimal.ZERO;
        try {
            GenericValue reservation = EntityUtil.getFirst(this.getDelegator().findByAnd("OrderItemAndShipGrpInvResAndItemSum",
                    UtilMisc.toMap("orderId", orderId, "orderItemSeqId", orderItemSeqId, "shipGroupSeqId", shipGroupSeqId), null, false));
            reservedQty = reservation.getBigDecimal("totQuantityAvailable");
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return reservedQty;
    }

    /**
     * Check verified qty.
     * @param orderId the order id
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    protected void checkVerifiedQty(String orderId, Locale locale) throws GeneralException {

        BigDecimal verifiedQty = BigDecimal.ZERO;
        BigDecimal orderedQty = BigDecimal.ZERO;

        List<GenericValue> orderItems = this.getDelegator().findByAnd("OrderItem", UtilMisc.toMap("orderId", orderId,
                "statusId", "ITEM_APPROVED"), null, false);
        for (GenericValue orderItem : orderItems) {
            orderedQty = orderedQty.add(orderItem.getBigDecimal("quantity"));
        }

        for (VerifyPickSessionRow pickRow : this.getPickRows(orderId)) {
            verifiedQty = verifiedQty.add(pickRow.getReadyToVerifyQty());
        }

        if (orderedQty.compareTo(verifiedQty) != 0) {
            throw new GeneralException(UtilProperties.getMessage("ProductErrorUiLabels", "ProductErrorAllOrderItemsAreNotVerified", locale));
        }
    }

    /**
     * Issue items to shipment.
     * @param shipmentId the shipment id
     * @param locale the locale
     * @throws GeneralException the general exception
     */
    protected void issueItemsToShipment(String shipmentId, Locale locale) throws GeneralException {
        List<VerifyPickSessionRow> processedRows = new LinkedList<>();
        for (VerifyPickSessionRow pickRow : this.getPickRows()) {
            if (this.checkLine(processedRows, pickRow)) {
                BigDecimal totalVerifiedQty = this.getVerifiedQuantity(pickRow.getOrderId(), pickRow.getOrderItemSeqId(),
                        pickRow.getShipGroupSeqId(), pickRow.getProductId(), pickRow.getInventoryItemId());
                pickRow.issueItemToShipment(shipmentId, picklistBinId, userLogin, totalVerifiedQty, getDispatcher(), locale);
                processedRows.add(pickRow);
            }
        }
    }

    /**
     * Check line boolean.
     * @param processedRows the processed rows
     * @param pickrow the pickrow
     * @return the boolean
     */
    protected boolean checkLine(List<VerifyPickSessionRow> processedRows, VerifyPickSessionRow pickrow) {
        for (VerifyPickSessionRow processedRow : processedRows) {
            if (pickrow.isSameItem(processedRow)) {
                pickrow.setShipmentItemSeqId(processedRow.getShipmentItemSeqId());
                return false;
            }
        }
        return true;
    }

    /**
     * Create shipment string.
     * @param line the line
     * @return the string
     * @throws GeneralException the general exception
     */
    protected String createShipment(VerifyPickSessionRow line) throws GeneralException {
        Delegator delegator = this.getDelegator();
        String orderId = line.getOrderId();
        Map<String, Object> newShipment = new HashMap<>();
        newShipment.put("originFacilityId", facilityId);
        newShipment.put("primaryShipGroupSeqId", line.getShipGroupSeqId());
        newShipment.put("primaryOrderId", orderId);
        newShipment.put("shipmentTypeId", "OUTGOING_SHIPMENT");
        newShipment.put("statusId", "SHIPMENT_SCHEDULED");
        newShipment.put("userLogin", this.getUserLogin());
        GenericValue orderRoleShipTo = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "roleTypeId",
                "SHIP_TO_CUSTOMER").queryFirst();
        if (UtilValidate.isNotEmpty(orderRoleShipTo)) {
            newShipment.put("partyIdTo", orderRoleShipTo.getString("partyId"));
        }
        String partyIdFrom = null;
        GenericValue orderItemShipGroup = EntityQuery.use(delegator).from("OrderItemShipGroup").where("orderId", orderId, "shipGroupSeqId",
                line.getShipGroupSeqId()).queryFirst();
        if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("vendorPartyId"))) {
            partyIdFrom = orderItemShipGroup.getString("vendorPartyId");
        } else if (UtilValidate.isNotEmpty(orderItemShipGroup.getString("facilityId"))) {
            GenericValue facility = EntityQuery.use(delegator).from("Facility").where("facilityId",
                    orderItemShipGroup.getString("facilityId")).queryOne();
            if (UtilValidate.isNotEmpty(facility.getString("ownerPartyId"))) {
                partyIdFrom = facility.getString("ownerPartyId");
            }
        }
        if (UtilValidate.isEmpty(partyIdFrom)) {
            GenericValue orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId,
                    "roleTypeId", "SHIP_FROM_VENDOR").queryFirst();
            if (UtilValidate.isNotEmpty(orderRoleShipFrom)) {
                partyIdFrom = orderRoleShipFrom.getString("partyId");
            } else {
                orderRoleShipFrom = EntityQuery.use(delegator).from("OrderRole").where("orderId", orderId, "roleTypeId",
                        "BILL_FROM_VENDOR").queryFirst();
                partyIdFrom = orderRoleShipFrom.getString("partyId");
            }
        }
        newShipment.put("partyIdFrom", partyIdFrom);
        Map<String, Object> newShipResp = this.getDispatcher().runSync("createShipment", newShipment);
        if (ServiceUtil.isError(newShipResp)) {
            throw new GeneralException(ServiceUtil.getErrorMessage(newShipResp));
        }
        String shipmentId = (String) newShipResp.get("shipmentId");
        return shipmentId;
    }

    /**
     * Update product.
     * @throws GeneralException the general exception
     */
    protected void updateProduct() throws GeneralException {
        for (VerifyPickSessionRow pickRow : this.getPickRows()) {
            if (UtilValidate.isNotEmpty(pickRow.getOriginGeoId())) {
                Map<String, Object> updateProductCtx = new HashMap<>();
                updateProductCtx.put("originGeoId", pickRow.getOriginGeoId());
                updateProductCtx.put("productId", pickRow.getProductId());
                updateProductCtx.put("userLogin", this.getUserLogin());
                Map<String, Object> result = this.getDispatcher().runSync("updateProduct", updateProductCtx);
                if (ServiceUtil.isError(result)) {
                    throw new GeneralException(ServiceUtil.getErrorMessage(result));
                }
            }
        }
    }
}
