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
package org.sitenetsoft.sunseterp.applications.order.shoppingcart;

import org.sitenetsoft.sunseterp.framework.base.util.UtilHttp;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.applications.product.store.ProductStoreWorker;
import org.sitenetsoft.sunseterp.framework.webapp.website.WebSiteWorker;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;

/**
 * WebShoppingCart.java
 *
 * Extension of {@link org.sitenetsoft.sunseterp.applications.order.shoppingcart.ShoppingCart ShoppingCart}
 * class which provides web presentation layer specific functionality
 * related specifically to user session information.
 */
@SuppressWarnings("serial")
public class WebShoppingCart extends ShoppingCart {
    public WebShoppingCart(HttpServletRequest request, Locale locale, String currencyUom) {
        // for purchase orders, bill to customer partyId must be set - otherwise, no way to know who we're purchasing for.
        // supplierPartyId is furnished by order manager for PO entry.
        // TODO: refactor constructor and the getCartObject method which calls them to multiple constructors for different types of orders
        super((Delegator) request.getAttribute("delegator"), ProductStoreWorker.getProductStoreId(request),
                WebSiteWorker.getWebSiteId(request), (locale != null ? locale : ProductStoreWorker.getStoreLocale(request)), (currencyUom != null
                        ? currencyUom : ProductStoreWorker.getStoreCurrencyUomId(request)), request.getParameter("billToCustomerPartyId"), (
                        request.getParameter("supplierPartyId") != null ? request.getParameter("supplierPartyId")
                        : request.getParameter("billFromVendorPartyId")));

        HttpSession session = request.getSession(true);
        this.setUserLogin((GenericValue) session.getAttribute("userLogin"));
        this.setAutoUserLogin((GenericValue) session.getAttribute("autoUserLogin"));
        this.setOrderPartyId((String) session.getAttribute("orderPartyId"));
    }

    public WebShoppingCart(HttpServletRequest request) {
        this(request, UtilHttp.getLocale(request), UtilHttp.getCurrencyUom(request));
    }

    /** Creates a new cloned ShoppingCart Object. */
    public WebShoppingCart(ShoppingCart cart) {
        super(cart);
    }
}
