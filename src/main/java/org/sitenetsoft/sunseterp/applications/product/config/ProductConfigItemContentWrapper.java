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
package org.sitenetsoft.sunseterp.applications.product.config;

import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.StringUtil.StringWrapper;
import org.sitenetsoft.sunseterp.framework.base.util.cache.UtilCache;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWorker;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWrapper;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.DelegatorFactory;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelEntity;
import org.sitenetsoft.sunseterp.framework.entity.model.ModelUtil;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityUtilProperties;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.service.ServiceContainer;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Product Config Item Content Worker: gets product content to display
 */
public class ProductConfigItemContentWrapper implements ContentWrapper {

    private static final String MODULE = ProductConfigItemContentWrapper.class.getName();
    public static final String SEPARATOR = "::";    // cache key separator
    private static final UtilCache<String, String> CONFIG_ITEM_CONTENT_CACHE = UtilCache.createUtilCache("configItem.content", true);
    // use soft reference to free up memory if needed

    private transient LocalDispatcher dispatcher;
    private String dispatcherName;
    private transient Delegator delegator;
    private String delegatorName;
    private GenericValue productConfigItem;
    private Locale locale;
    private String mimeTypeId;


    public static ProductConfigItemContentWrapper makeProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        return new ProductConfigItemContentWrapper(productConfigItem, request);
    }

    public ProductConfigItemContentWrapper(LocalDispatcher dispatcher, GenericValue productConfigItem, Locale locale, String mimeTypeId) {
        this.dispatcher = dispatcher;
        this.dispatcherName = dispatcher.getName();
        this.delegator = productConfigItem.getDelegator();
        this.delegatorName = delegator.getDelegatorName();
        this.productConfigItem = productConfigItem;
        this.locale = locale;
        this.mimeTypeId = mimeTypeId;
    }

    public ProductConfigItemContentWrapper(GenericValue productConfigItem, HttpServletRequest request) {
        this.dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        this.dispatcherName = dispatcher.getName();
        this.delegator = (Delegator) request.getAttribute("delegator");
        this.delegatorName = delegator.getDelegatorName();
        this.productConfigItem = productConfigItem;
        this.locale = UtilHttp.getLocale(request);
        this.mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", this.delegator);
    }

    @Override
    public StringWrapper get(String confItemContentTypeId, String encoderType) {
        return StringUtil.makeStringWrapper(getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, mimeTypeId,
                getDelegator(), getDispatcher(), encoderType));
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
     * Gets dispatcher.
     * @return the dispatcher
     */
    public LocalDispatcher getDispatcher() {
        if (dispatcher == null) {
            dispatcher = ServiceContainer.getLocalDispatcher(dispatcherName, this.getDelegator());
        }
        return dispatcher;
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId,
                                                           HttpServletRequest request, String encoderType) {
        LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
        String mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8",
                productConfigItem.getDelegator());
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, UtilHttp.getLocale(request), mimeTypeId,
                productConfigItem.getDelegator(), dispatcher, encoderType);
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale,
                                                           LocalDispatcher dispatcher, String encoderType) {
        return getProductConfigItemContentAsText(productConfigItem, confItemContentTypeId, locale, null, null, dispatcher, encoderType);
    }

    public static String getProductConfigItemContentAsText(GenericValue productConfigItem, String confItemContentTypeId, Locale locale,
                                                           String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, String encoderType) {
        UtilCodec.SimpleEncoder encoder = UtilCodec.getEncoder(encoderType);
        String candidateFieldName = ModelUtil.dbNameToVarName(confItemContentTypeId);
        String cacheKey = confItemContentTypeId + SEPARATOR + locale + SEPARATOR + mimeTypeId + SEPARATOR + productConfigItem.get("configItemId")
                + SEPARATOR + encoderType + SEPARATOR + delegator;
        try {
            String cachedValue = CONFIG_ITEM_CONTENT_CACHE.get(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }
            Writer outWriter = new StringWriter();
            getProductConfigItemContentAsText(null, productConfigItem, confItemContentTypeId, locale, mimeTypeId, delegator, dispatcher,
                    outWriter, false);
            String outString = outWriter.toString();
            if (UtilValidate.isEmpty(outString)) {
                outString = productConfigItem.getModelEntity().isField(candidateFieldName) ? productConfigItem.getString(candidateFieldName) : "";
                outString = outString == null ? "" : outString;
            }
            outString = encoder.sanitize(outString, null);
            CONFIG_ITEM_CONTENT_CACHE.put(cacheKey, outString);
            return outString;
        } catch (GeneralException | IOException e) {
            Debug.logError(e, "Error rendering ProdConfItemContent, inserting empty String", MODULE);
            String candidateOut = productConfigItem.getModelEntity().isField(candidateFieldName)
                    ? productConfigItem.getString(candidateFieldName) : "";
            return candidateOut == null ? "" : encoder.sanitize(candidateOut, null);
        }
    }

    public static void getProductConfigItemContentAsText(String configItemId, GenericValue productConfigItem, String confItemContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter)
            throws GeneralException, IOException {
        getProductConfigItemContentAsText(configItemId, productConfigItem, confItemContentTypeId, locale, mimeTypeId, delegator, dispatcher,
                outWriter, true);
    }

    public static void getProductConfigItemContentAsText(String configItemId, GenericValue productConfigItem, String confItemContentTypeId,
            Locale locale, String mimeTypeId, Delegator delegator, LocalDispatcher dispatcher, Writer outWriter, boolean cache)
            throws GeneralException, IOException {
        if (configItemId == null && productConfigItem != null) {
            configItemId = productConfigItem.getString("configItemId");
        }

        if (delegator == null && productConfigItem != null) {
            delegator = productConfigItem.getDelegator();
        }

        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = EntityUtilProperties.getPropertyValue("content", "defaultMimeType", "text/html; charset=utf-8", delegator);
        }

        GenericValue productConfigItemContent = EntityQuery.use(delegator).from("ProdConfItemContent")
                .where("configItemId", configItemId, "confItemContentTypeId", confItemContentTypeId)
                .orderBy("-fromDate")
                .cache(cache)
                .filterByDate()
                .queryFirst();
        if (productConfigItemContent != null) {
            // when rendering the product config item content, always include the ProductConfigItem and
            // ProdConfItemContent records that this comes from
            Map<String, Object> inContext = new HashMap<>();
            inContext.put("productConfigItem", productConfigItem);
            inContext.put("productConfigItemContent", productConfigItemContent);
            ContentWorker.renderContentAsText(dispatcher, productConfigItemContent.getString("contentId"), outWriter, inContext, locale,
                    mimeTypeId, null, null, cache);
            return;
        }

        String candidateFieldName = ModelUtil.dbNameToVarName(confItemContentTypeId);
        ModelEntity productConfigItemModel = delegator.getModelEntity("ProductConfigItem");
        if (productConfigItemModel.isField(candidateFieldName)) {
            if (productConfigItem == null) {
                productConfigItem = EntityQuery.use(delegator).from("ProductConfigItem").where("configItemId", configItemId).cache().queryOne();
            }
            if (productConfigItem != null) {
                String candidateValue = productConfigItem.getString(candidateFieldName);
                if (UtilValidate.isNotEmpty(candidateValue)) {
                    outWriter.write(candidateValue);
                    return;
                }
            }
        }
    }
}

