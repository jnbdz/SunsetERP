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
package org.sitenetsoft.sunseterp.applications.product.product;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.cache.UtilCache;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWorker;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWrapper;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityCondition;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityExpr;
import org.sitenetsoft.sunseterp.framework.entity.condition.EntityOperator;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtilProperties;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * Product Promo Content Worker: gets product promo content to display
 */
public class ProductPromoContentWrapper implements ContentWrapper {

    private static final String MODULE = ProductPromoContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator

    private static final UtilCache<String, String> PRODUCT_PROMO_CONTENT_CACHE =
            UtilCache.createUtilCache("product.promo.content.rendered", true);

    public static ProductPromoContentWrapper makeProductPromoContentWrapper(GenericValue productPromo, HttpServletRequest request) {
        return new ProductPromoContentWrapper(productPromo, request);
    }

    private LocalDispatcher dispatcher;
    private GenericValue productPromo;
    private Locale locale;
    private String mimeTypeId;

    public ProductPromoContentWrapper(LocalDispatcher dispatcher, GenericValue productPromo, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.productPromo = productPromo;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductPromoContentWrapper(GenericValue productPromo, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.productPromo = productPromo;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8",
                (Delegator) request.getAttribute("delegator"));
    }

    @Override
    public StringUtil.StringWrapper get(String productPromoContentTypeId, String encoderType) {
        if (UtilValidate.isEmpty(this.productPromo)) {
            Debug.logWarning("Tried to get ProductPromoContent for type [" + productPromoContentTypeId
                    + "] but the productPromo field in the ProductPromoContentWrapper is null", MODULE);
            return null;
        }
        return StringUtil.makeStringWrapper(getProductPromoContentAsText(this.productPromo, productPromoContentTypeId, locale, mimeTypeId, null,
                null, this.productPromo.getDelegator(), dispatcher, encoderType));
    }

    public static String getProductPromoContentAsText(GenericValue productPromo, String productPromoContentTypeId, HttpServletRequest request,
                                                      String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        return getProductPromoContentAsText(productPromo, productPromoContentTypeId, UtilHttp.getLocale(request),
                EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator),
                null, null, productPromo.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductContentAsText(GenericValue productPromo, String productPromoContentTypeId, Locale locale, LocalDispatcher
            dispatcher, String encoderType) {
        return getProductPromoContentAsText(productPromo, productPromoContentTypeId, locale, null, null, null, null, dispatcher, encoderType);
    }

    public static String getProductPromoContentAsText(GenericValue productPromo, String productPromoContentTypeId, Locale locale, String mimeTypeId,
               String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        if (UtilValidate.isEmpty(productPromo)) {
            return null;
        }

        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String candidateFieldName = ModelUtil.dbNameToVarName(productPromoContentTypeId);
        /* caching: there is one cache created, "product.promo.content"  Each productPromo's content is cached with a key of
         * contentTypeId::locale::mimeType::productPromoId, or whatever the SEPARATOR is defined above to be.
         */
        String cacheKey = productPromoContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + productPromo.get("productPromoId")
                + SEPARATOR + encoderType + SEPARATOR + delegator;
        try {
            String cachedValue = PRODUCT_PROMO_CONTENT_CACHE.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }

            Writer outWriter = new StringWriter();
            getProductPromoContentAsText(null, productPromo, productPromoContentTypeId, locale, mimeTypeId, partyId, roleTypeId,
                    delegator, dispatcher, outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = productPromo.getModelEntity().isField(candidateFieldName) ? productPromo.getString(candidateFieldName) : "";
                outString = outString == null ? "" : outString;
            }
            outString = encoder.sanitize(outString, null);
            PRODUCT_PROMO_CONTENT_CACHE.put(cacheKey, outString);
            return outString;
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering ProductPromoContent, inserting empty String", MODULE);
            String candidateOut = productPromo.getModelEntity().isField(candidateFieldName) ? productPromo.getString(candidateFieldName) : "";
            return candidateOut == null ? "" : encoder.sanitize(candidateOut, null);
        }
    }

    public static void getProductPromoContentAsText(String productPromoId, GenericValue productPromo, String productPromoContentTypeId,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter)
            throws GeneralException, IOException {
        getProductPromoContentAsText(productPromoId, productPromo, productPromoContentTypeId, locale, mimeTypeId, partyId, roleTypeId, delegator,
                dispatcher, outWriter, true);
    }

    public static void getProductPromoContentAsText(String productPromoId, GenericValue productPromo, String productPromoContentTypeId,
            Locale locale, String mimeTypeId, String partyId, String roleTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter,
                                                    boolean cache) throws GeneralException, IOException {
        if (UtilValidate.isEmpty(productPromoId) && productPromo != null) {
            productPromoId = productPromo.getString("productPromoId");
        }

        if (UtilValidate.isEmpty(delegator) && productPromo != null) {
            delegator = productPromo.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        if (UtilValidate.isEmpty(delegator)) {
            throw new GeneralRuntimeException("Unable to find a delegator to use!");
        }

        List<EntityExpr> exprs = new ArrayList<>();
        exprs.add(EntityCondition.makeCondition("productPromoId", EntityOperator.EQUALS, productPromoId));
        exprs.add(EntityCondition.makeCondition("productPromoContentTypeId", EntityOperator.EQUALS, productPromoContentTypeId));

        List<GenericValue> productPromoContentList = EntityQuery.use(delegator).from("ProductPromoContent").where(EntityCondition
                .makeCondition(exprs, EntityOperator.AND)).orderBy("-fromDate").cache(cache).queryList();
        GenericValue productPromoContent = null;
        if (UtilValidate.isNotEmpty(productPromoContentList)) {
            productPromoContent = EntityUtil.getFirst(EntityUtil.filterByDate(productPromoContentList));
        }

        if (productPromoContent != null) {
            // when rendering the product promo content, always include the ProductPromo and ProductPromoContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("productPromo", productPromo);
            inContext.put("productPromoContent", productPromoContent);
            ContentWorker.renderContentAsText(dispatcher, productPromoContent.getString("contentId"), outWriter,
                    inContext, locale, mimeTypeId, partyId, roleTypeId, cache);
            return;
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(productPromoContentTypeId);
        ModelEntity productModel = delegator.getModelEntity("ProductPromo");
        if (productModel.isField(candidateFieldName)) {
            if (UtilValidate.isEmpty(productPromo)) {
                productPromo = EntityQuery.use(delegator).from("ProductPromo").where("productPromoId", productPromoId).cache().queryOne();
            }
            if (productPromo != null) {
                String candidateValue = productPromo.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
    }
}
