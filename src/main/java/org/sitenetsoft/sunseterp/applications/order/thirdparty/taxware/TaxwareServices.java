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
package org.sitenetsoft.sunseterp.applications.order.thirdparty.taxware;

// TODO: Double check what lib to use for BigDecimal
//import com.ibm.icu.math.BigDecimal;
import java.math.BigDecimal;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.service.DispatchContext;
import org.sitenetsoft.sunseterp.framework.service.ModelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TaxwareServices
 */
public class TaxwareServices {

    private static final String MODULE = TaxwareServices.class.getName();

    /*public static Map calcTax(DispatchContext dctx, Map context) {
        Map result = new HashMap();
        List items = (List) context.get("itemProductList");
        List amnts = (List) context.get("itemAmountList");
        List ishpn = (List) context.get("itemShippingList");
        BigDecimal shipping = (BigDecimal) context.get("orderShippingAmount");
        GenericValue address = (GenericValue) context.get("shippingAddress");

        if (items.size() != amnts.size()) {
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Items, Amount, or ItemShipping lists are not valid size.");
            return result;
        }

        try {
            TaxwareUTL utl = new TaxwareUTL();

            utl.setShipping(shipping != null ? shipping : BigDecimal.ZERO);
            utl.setShipAddress(address);
            for (int i = 0; i < items.size(); i++) {
                GenericValue p = (GenericValue) items.get(i);
                BigDecimal amount = (BigDecimal) amnts.get(i);
                BigDecimal ishp = ishpn != null ? (BigDecimal) ishpn.get(i) : BigDecimal.ZERO;

                utl.addItem(p, amount, ishp);
            }

            int resp = utl.process();

            if (resp == 0) {
                result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
                result.put(ModelService.ERROR_MESSAGE, "ERROR: No records processed.");
                return result;
            }

            result.put("orderAdjustments", utl.getOrderAdjustments());
            result.put("itemAdjustments", utl.getItemAdjustments());

        } catch (TaxwareException e) {
            Debug.logError(e, MODULE);
            result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_ERROR);
            result.put(ModelService.ERROR_MESSAGE, "ERROR: Taxware problem (" + e.getMessage() + ").");
        }

        return result;
    }

    public static Map verifyZip(DispatchContext dctx, Map context) {

        return new HashMap();
    }*/
}
