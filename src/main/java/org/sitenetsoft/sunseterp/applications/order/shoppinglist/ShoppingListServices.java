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
package org.sitenetsoft.sunseterp.applications.order.shoppinglist;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.collections.PagedList;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.transaction.GenericTransactionException;
import org.sitenetsoft.sunseterp.framework.entity.transaction.TransactionUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.*;
import org.sitenetsoft.sunseterp.applications.order.order.OrderReadHelper;
import org.sitenetsoft.sunseterp.applications.order.shoppingcart.CartItemModifyException;
import org.sitenetsoft.sunseterp.applications.order.shoppingcart.CheckOutHelper;
import org.sitenetsoft.sunseterp.applications.order.shoppingcart.ItemNotFoundException;
import org.sitenetsoft.sunseterp.applications.order.shoppingcart.ShoppingCart;
import org.sitenetsoft.sunseterp.applications.product.config.ProductConfigWorker;
import org.sitenetsoft.sunseterp.applications.product.config.ProductConfigWrapper;
import org.sitenetsoft.sunseterp.applications.product.product.ProductWorker;
import org.sitenetsoft.sunseterp.applications.product.store.ProductStoreWorker;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.GenericServiceException;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceUtil;
import org.sitenetsoft.sunseterp.framework.service.calendar.RecurrenceInfo;
import org.sitenetsoft.sunseterp.framework.service.calendar.RecurrenceInfoException;

import javax.transaction.Transaction;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shopping List Services
 */
public class ShoppingListServices {

    private static final String MODULE = ShoppingListServices.class.getName();
    private static final String RES_ERROR = "OrderErrorUiLabels";

    public static Map<String, Object> setShoppingListRecurrence(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");
        Locale locale = (Locale) context.get("locale");

        if (frequency == null || interval == null) {
            Debug.logWarning(UtilProperties.getMessage(RES_ERROR, "OrderFrequencyOrIntervalWasNotSpecified", locale), MODULE);
            return ServiceUtil.returnSuccess();
        }

        if (startDate == null) {
            switch (frequency) {
            case 5:
                startDate = UtilDateTime.getWeekStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            case 6:
                startDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            case 7:
                startDate = UtilDateTime.getYearStart(UtilDateTime.nowTimestamp(), 0, interval);
                break;
            default:
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderInvalidFrequencyForShoppingListRecurrence", locale));
            }
        }

        long startTime = startDate.getTime();
        long endTime = 0;
        if (endDate != null) {
            endTime = endDate.getTime();
        }

        RecurrenceInfo recInfo = null;
        try {
            recInfo = RecurrenceInfo.makeInfo(delegator, startTime, frequency, interval, -1, endTime);
        } catch (RecurrenceInfoException e) {
            Debug.logError(e, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToCreateShoppingListRecurrenceInformation", locale));
        }

        Debug.logInfo("Next Recurrence - " + UtilDateTime.getTimestamp(recInfo.next()), MODULE);
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("recurrenceInfoId", recInfo.getID());

        return result;
    }

    public static Map<String, Object> createListReorders(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean beganTransaction = false;
        EntityQuery eq = EntityQuery.use(delegator)
                .from("ShoppingList")
                .where("shoppingListTypeId", "SLT_AUTO_REODR", "isActive", "Y")
                .orderBy("-lastOrderedDate");
        try {
            beganTransaction = TransactionUtil.begin();
        } catch (GenericTransactionException e1) {
            Debug.logError(e1, "[Delegator] Could not begin transaction: " + e1.toString(), MODULE);
        }

        try (EntityListIterator eli = eq.queryIterator()) {
            if (eli != null) {
                GenericValue shoppingList;
                while (((shoppingList = eli.next()) != null)) {
                    Timestamp lastOrder = shoppingList.getTimestamp("lastOrderedDate");
                    RecurrenceInfo recurrence = null;

                    GenericValue recurrenceInfo = shoppingList.getRelatedOne("RecurrenceInfo", false);
                    Timestamp startDateTime = recurrenceInfo.getTimestamp("startDateTime");

                    try {
                        recurrence = new RecurrenceInfo(recurrenceInfo);
                    } catch (RecurrenceInfoException e) {
                        Debug.logError(e, MODULE);
                    }


                    // check the next recurrence
                    if (recurrence != null) {
                        long next = lastOrder == null ? recurrence.next(startDateTime.getTime()) : recurrence.next(lastOrder.getTime());
                        Timestamp now = UtilDateTime.nowTimestamp();
                        Timestamp nextOrder = UtilDateTime.getDayStart(UtilDateTime.getTimestamp(next));

                        if (nextOrder.after(now)) {
                            continue;
                        }
                    } else {
                        continue;
                    }

                    ShoppingCart listCart = makeShoppingListCart(dispatcher, shoppingList, locale);
                    CheckOutHelper helper = new CheckOutHelper(dispatcher, delegator, listCart);

                    // store the order
                    Map<String, Object> createResp = helper.createOrder(userLogin);
                    if (createResp == null || ServiceUtil.isError(createResp)) {
                        Debug.logError("Cannot create order for shopping list - " + shoppingList, MODULE);
                    } else {

                        String orderId = (String) createResp.get("orderId");

                        // authorize the payments
                        Map<String, Object> payRes = null;
                        try {
                            payRes = helper.processPayment(ProductStoreWorker.getProductStore(listCart.getProductStoreId(), delegator), userLogin);
                        } catch (GeneralException e) {
                            Debug.logError(e, MODULE);
                        }

                        if (payRes != null && ServiceUtil.isError(payRes)) {
                            Debug.logError("Payment processing problems with shopping list - " + shoppingList, MODULE);
                        }

                        shoppingList.set("lastOrderedDate", UtilDateTime.nowTimestamp());
                        shoppingList.store();

                        // send notification
                        try {
                            dispatcher.runAsync("sendOrderPayRetryNotification", UtilMisc.toMap("orderId", orderId));
                        } catch (GenericServiceException e) {
                            Debug.logError(e, MODULE);
                        }

                        // increment the recurrence
                        recurrence.incrementCurrentCount();
                    }
                }
            }

            return ServiceUtil.returnSuccess();
        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error creating shopping list auto-reorders", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[Delegator] Could not rollback transaction: " + e2.toString(), MODULE);
            }

            String errMsg = UtilProperties.getMessage(RES_ERROR, "OrderErrorWhileCreatingNewShoppingListBasedAutomaticReorder", UtilMisc.toMap(
                    "errorString", e.toString()), locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based automatic reorder", MODULE);
            }
        }
    }

    public static Map<String, Object> splitShipmentMethodString(DispatchContext dctx, Map<String, ? extends Object> context) {
        String shipmentMethodString = (String) context.get("shippingMethodString");
        Map<String, Object> result = ServiceUtil.returnSuccess();

        if (UtilValidate.isNotEmpty(shipmentMethodString)) {
            int delimiterPos = shipmentMethodString.indexOf('@');
            String shipmentMethodTypeId = null;
            String carrierPartyId = null;

            if (delimiterPos > 0) {
                shipmentMethodTypeId = shipmentMethodString.substring(0, delimiterPos);
                carrierPartyId = shipmentMethodString.substring(delimiterPos + 1);
                result.put("shipmentMethodTypeId", shipmentMethodTypeId);
                result.put("carrierPartyId", carrierPartyId);
            }
        }
        return result;
    }

    public static Map<String, Object> makeListFromOrder(DispatchContext dctx, Map<String, ? extends Object> context) {
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        String shoppingListTypeId = (String) context.get("shoppingListTypeId");
        String shoppingListId = (String) context.get("shoppingListId");
        String orderId = (String) context.get("orderId");
        String partyId = (String) context.get("partyId");

        Timestamp startDate = (Timestamp) context.get("startDateTime");
        Timestamp endDate = (Timestamp) context.get("endDateTime");
        Integer frequency = (Integer) context.get("frequency");
        Integer interval = (Integer) context.get("intervalNumber");

        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");

        boolean beganTransaction = false;
        try {
            beganTransaction = TransactionUtil.begin();

            GenericValue orderHeader = null;
            orderHeader = EntityQuery.use(delegator).from("OrderHeader").where("orderId", orderId).queryOne();

            if (orderHeader == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToLocateOrder", UtilMisc.toMap("orderId", orderId),
                        locale));
            }
            String productStoreId = orderHeader.getString("productStoreId");

            if (UtilValidate.isEmpty(shoppingListId)) {
                // create a new shopping list
                if (partyId == null) {
                    partyId = userLogin.getString("partyId");
                }

                Map<String, Object> serviceCtx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "partyId", partyId,
                        "productStoreId", productStoreId, "listName", "List Created From Order #" + orderId);

                if (UtilValidate.isNotEmpty(shoppingListTypeId)) {
                    serviceCtx.put("shoppingListTypeId", shoppingListTypeId);
                }

                Map<String, Object> newListResult = null;
                try {

                    newListResult = dispatcher.runSync("createShoppingList", serviceCtx, 90, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, "Problems creating new ShoppingList", MODULE);
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToCreateNewShoppingList", locale));
                }

                // check for errors
                if (ServiceUtil.isError(newListResult)) {
                    return ServiceUtil.returnError(ServiceUtil.getErrorMessage(newListResult));
                }

                // get the new list id
                if (newListResult != null) {
                    shoppingListId = (String) newListResult.get("shoppingListId");
                }
            }

            GenericValue shoppingList = null;
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();

            if (shoppingList == null) {
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderNoShoppingListAvailable", locale));
            }
            shoppingListTypeId = shoppingList.getString("shoppingListTypeId");

            OrderReadHelper orh;
            try {
                orh = new OrderReadHelper(orderHeader);
            } catch (IllegalArgumentException e) {
                Debug.logError(e, MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToLoadOrderReadHelper", UtilMisc.toMap("orderId",
                        orderId), locale));
            }

            List<GenericValue> orderItems = orh.getOrderItems();
            for (GenericValue orderItem : orderItems) {
                String productId = orderItem.getString("productId");
                if (UtilValidate.isNotEmpty(productId)) {
                    Map<String, Object> ctx = UtilMisc.<String, Object>toMap("userLogin", userLogin, "shoppingListId", shoppingListId, "productId",
                            orderItem.get("productId"), "quantity", orderItem.get("quantity"));
                    if (EntityTypeUtil.hasParentType(delegator, "ProductType", "productTypeId", ProductWorker.getProductTypeId(delegator,
                            productId), "parentTypeId", "AGGREGATED")) {
                        try {
                            GenericValue instanceProduct = EntityQuery.use(delegator).from("Product").where("productId", productId).queryOne();
                            String configId = instanceProduct.getString("configId");
                            ctx.put("configId", configId);
                            String aggregatedProductId = ProductWorker.getInstanceAggregatedId(delegator, productId);
                            //override the instance productId with aggregated productId
                            ctx.put("productId", aggregatedProductId);
                        } catch (GenericEntityException e) {
                            Debug.logError(e, MODULE);
                        }
                    }
                    Map<String, Object> serviceResult = null;
                    try {
                        serviceResult = dispatcher.runSync("createShoppingListItem", ctx);
                    } catch (GenericServiceException e) {
                        Debug.logError(e, MODULE);
                    }
                    if (serviceResult == null || ServiceUtil.isError(serviceResult)) {
                        return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToAddItemToShoppingList", UtilMisc.toMap(
                                "shoppingListId", shoppingListId), locale));
                    }
                }
            }

            if ("SLT_AUTO_REODR".equals(shoppingListTypeId)) {
                GenericValue paymentPref = EntityUtil.getFirst(orh.getPaymentPreferences());
                GenericValue shipGroup = EntityUtil.getFirst(orh.getOrderItemShipGroups());

                Map<String, Object> slCtx = new HashMap<>();
                slCtx.put("shipmentMethodTypeId", shipGroup.get("shipmentMethodTypeId"));
                slCtx.put("carrierRoleTypeId", shipGroup.get("carrierRoleTypeId"));
                slCtx.put("carrierPartyId", shipGroup.get("carrierPartyId"));
                slCtx.put("contactMechId", shipGroup.get("contactMechId"));
                slCtx.put("paymentMethodId", paymentPref.get("paymentMethodId"));
                slCtx.put("currencyUom", orh.getCurrency());
                slCtx.put("startDateTime", startDate);
                slCtx.put("endDateTime", endDate);
                slCtx.put("frequency", frequency);
                slCtx.put("intervalNumber", interval);
                slCtx.put("isActive", "Y");
                slCtx.put("shoppingListId", shoppingListId);
                slCtx.put("userLogin", userLogin);

                Map<String, Object> slUpResp = null;
                try {
                    slUpResp = dispatcher.runSync("updateShoppingList", slCtx);
                } catch (GenericServiceException e) {
                    Debug.logError(e, MODULE);
                }

                if (slUpResp == null || ServiceUtil.isError(slUpResp)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RES_ERROR, "OrderUnableToUpdateShoppingListInformation",
                            UtilMisc.toMap("shoppingListId", shoppingListId), locale));
                }
            }

            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("shoppingListId", shoppingListId);
            return result;

        } catch (GenericEntityException e) {
            try {
                // only rollback the transaction if we started one...
                TransactionUtil.rollback(beganTransaction, "Error making shopping list from order", e);
            } catch (GenericEntityException e2) {
                Debug.logError(e2, "[Delegator] Could not rollback transaction: " + e2.toString(), MODULE);
            }

            String errMsg = UtilProperties.getMessage(RES_ERROR, "OrderErrorWhileCreatingNewShoppingListBasedOnOrder", UtilMisc.toMap("errorString",
                    e.toString()), locale);
            Debug.logError(e, errMsg, MODULE);
            return ServiceUtil.returnError(errMsg);
        } finally {
            try {
                // only commit the transaction if we started one... this will throw an exception if it fails
                TransactionUtil.commit(beganTransaction);
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not commit transaction for creating new shopping list based on order", MODULE);
            }
        }
    }

    /**
     * Create a new shoppingCart form a shoppingList
     * @param dispatcher   the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale       the locale in use
     * @return returns a new shopping cart form a shopping list
     */
    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        return makeShoppingListCart(null, dispatcher, shoppingList, locale);
    }

    /**
     * Add a shoppinglist to an existing shoppingcart
     * @param listCart     the shopping cart list
     * @param dispatcher   the local dispatcher
     * @param shoppingList a GenericValue object of the shopping list
     * @param locale       the locale in use
     * @return the modified shopping cart adding the shopping list elements
     */
    public static ShoppingCart makeShoppingListCart(ShoppingCart listCart, LocalDispatcher dispatcher, GenericValue shoppingList, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        if (shoppingList != null && shoppingList.get("productStoreId") != null) {
            String productStoreId = shoppingList.getString("productStoreId");
            String currencyUom = shoppingList.getString("currencyUom");
            if (currencyUom == null) {
                GenericValue productStore = ProductStoreWorker.getProductStore(productStoreId, delegator);
                if (productStore == null) {
                    return null;
                }
                currencyUom = productStore.getString("defaultCurrencyUomId");
            }
            if (locale == null) {
                locale = Locale.getDefault();
            }

            List<GenericValue> items = null;
            try {
                items = shoppingList.getRelated("ShoppingListItem", null, UtilMisc.toList("shoppingListItemSeqId"), false);
            } catch (GenericEntityException e) {
                Debug.logError(e, MODULE);
            }

            if (UtilValidate.isNotEmpty(items)) {
                if (listCart == null) {
                    listCart = new ShoppingCart(delegator, productStoreId, locale, currencyUom);
                    listCart.setOrderPartyId(shoppingList.getString("partyId"));
                    listCart.setAutoOrderShoppingListId(shoppingList.getString("shoppingListId"));
                } else {
                    if (!listCart.getPartyId().equals(shoppingList.getString("partyId"))) {
                        Debug.logError("CANNOT add shoppingList: " + shoppingList.getString("shoppingListId")
                                + " of partyId: " + shoppingList.getString("partyId")
                                + " to a shoppingcart with a different orderPartyId: "
                                + listCart.getPartyId(), MODULE);
                        return listCart;
                    }
                }


                ProductConfigWrapper configWrapper = null;
                for (GenericValue shoppingListItem : items) {
                    String productId = shoppingListItem.getString("productId");
                    BigDecimal quantity = shoppingListItem.getBigDecimal("quantity");
                    Timestamp reservStart = shoppingListItem.getTimestamp("reservStart");
                    BigDecimal reservLength = null;
                    String configId = shoppingListItem.getString("configId");

                    if (shoppingListItem.get("reservLength") != null) {
                        reservLength = shoppingListItem.getBigDecimal("reservLength");
                    }
                    BigDecimal reservPersons = null;
                    if (shoppingListItem.get("reservPersons") != null) {
                        reservPersons = shoppingListItem.getBigDecimal("reservPersons");
                    }
                    if (UtilValidate.isNotEmpty(productId) && quantity != null) {
                        if (UtilValidate.isNotEmpty(configId)) {
                            configWrapper = ProductConfigWorker.loadProductConfigWrapper(delegator, dispatcher, configId, productId,
                                    listCart.getProductStoreId(), null, listCart.getWebSiteId(), listCart.getCurrency(), listCart.getLocale(),
                                    listCart.getAutoUserLogin());
                        }
                        // list items are noted in the shopping cart
                        String listId = shoppingListItem.getString("shoppingListId");
                        String itemId = shoppingListItem.getString("shoppingListItemSeqId");
                        Map<String, Object> attributes = UtilMisc.<String, Object>toMap("shoppingListId", listId, "shoppingListItemSeqId", itemId);

                        try {
                            listCart.addOrIncreaseItem(productId, null, quantity, reservStart, reservLength, reservPersons, null, null, null, null,
                                    null, attributes, null, configWrapper, null, null, null, dispatcher);
                        } catch (CartItemModifyException e) {
                            Debug.logError(e, "Unable to add product to List Cart - " + productId, MODULE);
                        } catch (ItemNotFoundException e) {
                            Debug.logError(e, "Product not found - " + productId, MODULE);
                        }
                    }
                }

                if (listCart.size() > 0) {
                    if (UtilValidate.isNotEmpty(shoppingList.get("paymentMethodId"))) {
                        listCart.addPayment(shoppingList.getString("paymentMethodId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("contactMechId"))) {
                        listCart.setAllShippingContactMechId(shoppingList.getString("contactMechId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("shipmentMethodTypeId"))) {
                        listCart.setAllShipmentMethodTypeId(shoppingList.getString("shipmentMethodTypeId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.get("carrierPartyId"))) {
                        listCart.setAllCarrierPartyId(shoppingList.getString("carrierPartyId"));
                    }
                    if (UtilValidate.isNotEmpty(shoppingList.getString("productPromoCodeId"))) {
                        listCart.addProductPromoCode(shoppingList.getString("productPromoCodeId"), dispatcher);
                    }
                }
            }
        }
        return listCart;
    }

    public static ShoppingCart makeShoppingListCart(LocalDispatcher dispatcher, String shoppingListId, Locale locale) {
        Delegator delegator = dispatcher.getDelegator();
        GenericValue shoppingList = null;
        try {
            shoppingList = EntityQuery.use(delegator).from("ShoppingList").where("shoppingListId", shoppingListId).queryOne();
        } catch (GenericEntityException e) {
            Debug.logError(e, MODULE);
        }
        return makeShoppingListCart(dispatcher, shoppingList, locale);
    }

    /**
     * Given an orderId, this service will look through all its OrderItems and for each shoppingListItemId
     * and shoppingListItemSeqId, update the quantity purchased in the ShoppingListItem entity.  Used for
     * tracking how many of shopping list items are purchased.  This service is mounted as a seca on storeOrder.
     * @param ctx     - The DispatchContext that this service is operating in
     * @param context - Map containing the input parameters
     * @return Map with the result of the service, the output parameters
     */
    public static Map<String, Object> updateShoppingListQuantitiesFromOrder(DispatchContext ctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = new HashMap<>();
        Delegator delegator = ctx.getDelegator();
        String orderId = (String) context.get("orderId");
        try {
            List<GenericValue> orderItems = EntityQuery.use(delegator).from("OrderItem").where("orderId", orderId).queryList();
            for (GenericValue orderItem : orderItems) {
                String shoppingListId = orderItem.getString("shoppingListId");
                String shoppingListItemSeqId = orderItem.getString("shoppingListItemSeqId");
                if (UtilValidate.isNotEmpty(shoppingListId)) {
                    GenericValue shoppingListItem = EntityQuery.use(delegator).from("ShoppingListItem").where("shoppingListId", shoppingListId,
                            "shoppingListItemSeqId", shoppingListItemSeqId).queryOne();
                    if (shoppingListItem != null) {
                        BigDecimal quantityPurchased = shoppingListItem.getBigDecimal("quantityPurchased");
                        BigDecimal orderQuantity = orderItem.getBigDecimal("quantity");
                        if (quantityPurchased != null) {
                            shoppingListItem.set("quantityPurchased", orderQuantity.add(quantityPurchased));
                        } else {
                            shoppingListItem.set("quantityPurchased", orderQuantity);
                        }
                        shoppingListItem.store();
                    }
                }
            }
        } catch (GenericEntityException gee) {
            Debug.logInfo("updateShoppingListQuantitiesFromOrder error:" + gee.getMessage(), MODULE);
        }
        return result;
    }

    public static Map<String, Object> autoDeleteAutoSaveShoppingList(DispatchContext dctx, Map<String, ? extends Object> context) {
        Delegator delegator = dctx.getDelegator();
        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericValue userLogin = (GenericValue) context.get("userLogin");

        //set an upper limit for the number of pages to delete each run
        final int maxDeletePages = 50;

        int slProcessed = 0;
        int deleted = 0;
        int totPages = 0;
        int page = 1;
        int currentPage = page;

        //page size
        int limit;
        String limitStr = EntityUtilProperties.getPropertyValue("order", "autosave.delete.viewsize", "500", delegator);
        try {
            limit = Integer.parseInt(limitStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to get limit init it to 500", MODULE);
            limit = 500;
        }
        int viewSize = limit;

        int maxDays = 0;
        String maxDaysStr = EntityUtilProperties.getPropertyValue("order", "autosave.max.age", "30", delegator);
        try {
            maxDays = Integer.parseInt(maxDaysStr);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to get maxDays", MODULE);
        }
        if (maxDays <= 0) {
            return ServiceUtil.returnFailure("MaxDays define to " + maxDays + " nothing todo");
        }

        EntityListIterator iterator = null;
        Transaction parent = null;
        Timestamp deleteAllBefore = UtilDateTime.addDaysToTimestamp(UtilDateTime.nowTimestamp(), -maxDays);
        EntityCondition condDate = EntityCondition.makeCondition(UtilMisc.toList(
                EntityCondition.makeCondition(
                        EntityCondition.makeCondition(
                                EntityCondition.makeCondition("lastAdminModified", null),
                                EntityOperator.AND,
                                EntityCondition.makeCondition("lastUpdatedStamp", EntityOperator.LESS_THAN_EQUAL_TO, deleteAllBefore)),
                        EntityCondition.makeCondition("lastAdminModified", EntityOperator.LESS_THAN_EQUAL_TO, deleteAllBefore))),
                EntityOperator.OR);
        EntityCondition condParty = EntityCondition.makeConditionMap("partyId", null,
                        "shoppingListTypeId", "SLT_SPEC_PURP");
        EntityCondition cond = EntityCondition.makeCondition(condParty, condDate);

        try {
            iterator = EntityQuery.use(delegator)
                    .from("ShoppingList")
                    .where(cond)
                    .cursorScrollInsensitive()
                    .queryIterator();

            PagedList<GenericValue> shoppingListsPaged = null;
            List<GenericValue> shoppingLists = null;

            //initial values: upper limits for pages
            if (iterator != null) {

                shoppingListsPaged = EntityUtil.getPagedList(iterator, page, viewSize);
                int pagedListSize = shoppingListsPaged.getSize();

                totPages = pagedListSize / viewSize;
                if ((pagedListSize % viewSize) > 0) {
                    totPages++;
                }

                //close the iterator
                iterator.close();
                iterator = null;
            }

            if (shoppingListsPaged != null) {

                //suspend the current transaction; use an  internal one to commit the deletion of each page
                if (TransactionUtil.getStatus() != TransactionUtil.STATUS_NO_TRANSACTION) {
                    parent = TransactionUtil.suspend();
                }

                shoppingLists = shoppingListsPaged.getData();

                while (page <= maxDeletePages && page <= totPages) {
                    boolean beganTx = false;
                    try {

                        // begin transaction for this page: set a timeout of 300 (default is 60)
                        beganTx = TransactionUtil.begin(300);
                        if (page > currentPage) {

                            //Retrieve another page
                            iterator = EntityQuery.use(delegator)
                                    .from("ShoppingList")
                                    .where(cond)
                                    .cursorScrollInsensitive()
                                    .queryIterator();

                            shoppingListsPaged = EntityUtil.getPagedList(iterator, page, viewSize);
                            shoppingLists = shoppingListsPaged.getData();

                            iterator.close();
                            iterator = null;
                        }

                        //processing shopping lists
                        for (GenericValue sl : shoppingLists) {

                            List<GenericValue> shoppingListItems = null;
                            try {
                                shoppingListItems = sl.getRelated("ShoppingListItem", null, null, false);
                            } catch (GenericEntityException e) {
                                Debug.logError(e.getMessage(), MODULE);
                                TransactionUtil.rollback();
                                break;
                            }

                            for (GenericValue sli : shoppingListItems) {
                                try {
                                    dispatcher.runSync("removeShoppingListItem",
                                            UtilMisc.toMap("shoppingListId", sl.getString("shoppingListId"),
                                                    "shoppingListItemSeqId", sli.getString("shoppingListItemSeqId"),
                                                    "userLogin", userLogin));
                                } catch (GenericServiceException e) {
                                    Debug.logError(e.getMessage(), MODULE);
                                    TransactionUtil.rollback();
                                    break;
                                }
                            }
                            try {
                                dispatcher.runSync("removeShoppingList",
                                        UtilMisc.toMap("shoppingListId", sl.getString("shoppingListId"),
                                                "userLogin", userLogin));
                                deleted++;
                            } catch (GenericServiceException e) {
                                Debug.logError(e.getMessage(), MODULE);
                                TransactionUtil.rollback();
                                break;
                            }
                            slProcessed++;
                        }
                    } catch (GenericTransactionException gte) {
                        Debug.logError(gte.getMessage(), MODULE);
                        TransactionUtil.rollback();
                        break;
                    } finally {
                        //commit this page
                        if (beganTx) {
                            try {
                                TransactionUtil.commit(beganTx);
                            } catch (GenericTransactionException gte) {
                                Debug.logError(gte, "Unable to commit page " + page, MODULE);
                                TransactionUtil.rollback();
                                break;
                            }
                        }
                    }
                    currentPage = page;
                    page++;

                }
            }
        } catch (GenericEntityException e) {
            Debug.logError(e.getMessage(), MODULE);
            if (iterator != null) {
                try {
                    iterator.close();
                } catch (GenericEntityException ex) {
                    Debug.logError(ex, "Error occured in closing iterator.", MODULE);
                }
            }

            try {
                TransactionUtil.rollback();
            } catch (GenericTransactionException ex) {
                Debug.logError(ex, "Error in rolling back transaction", MODULE);
            }

            return ServiceUtil.returnError(e.getMessage());
        } finally {

            if (iterator != null) {
                try {
                    iterator.close();
                } catch (GenericEntityException ex) {
                    Debug.logError(ex, "Error occured in closing iterator.", MODULE);
                }
            }

            if (parent != null) {
                try {
                    TransactionUtil.resume(parent);
                } catch (GenericTransactionException e) {
                    Debug.logWarning(e, MODULE);
                }
            }
        }
        return ServiceUtil.returnSuccess(
                "Total shopping list processed [" + slProcessed + "] - "
                        + "shopping list deleted [" + deleted + "].");
    }
}
