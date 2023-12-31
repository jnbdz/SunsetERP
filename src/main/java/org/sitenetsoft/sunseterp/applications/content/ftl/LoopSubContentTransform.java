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
package org.sitenetsoft.sunseterp.applications.content.ftl;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;
import org.sitenetsoft.sunseterp.framework.base.util.*;
import org.sitenetsoft.sunseterp.framework.base.util.template.FreeMarkerWorker;
import org.sitenetsoft.sunseterp.applications.content.content.ContentServicesComplex;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWorker;
import org.sitenetsoft.sunseterp.framework.entity.Delegator;
import org.sitenetsoft.sunseterp.framework.entity.GenericEntityException;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.entity.util.EntityQuery;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;
import org.sitenetsoft.sunseterp.framework.webapp.ftl.LoopWriter;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LoopSubContentTransform - Freemarker Transform for URLs (links)
 */
public class LoopSubContentTransform implements TemplateTransformModel {

    private static final String MODULE = LoopSubContentTransform.class.getName();

    static final String[] SAVE_KEY_NAMES = {"contentId", "subContentId", "mimeType", "subContentDataResourceView", "wrapTemplateId",
            "contentTemplateId"};
    static final String[] REMOVE_KEY_NAMES = {"wrapTemplateId", "entityList", "entityIndex", "textData", "dataResourceId", "drDataResourceId",
            "subContentIdSub", "parentContent", "wrappedFTL"};

    /**
     * @deprecated use FreeMarkerWorker.getWrappedObject()
     * A wrapper for the FreeMarkerWorker version.
     */
    @Deprecated
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, Object> args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    /**
     * @deprecated use FreeMarkerWorker.getArg()
     */
    @Deprecated
    public static String getArg(Map<String, Object> args, String key, Map<String, Object> ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }

    public static boolean prepCtx(Delegator delegator, Map<String, Object> ctx) {
        List<GenericValue> lst = UtilGenerics.cast(ctx.get("entityList"));
        Integer idx = (Integer) ctx.get("entityIndex");
        if (idx == null) {
            idx = 0;
        }
        int i = idx;
        if (UtilValidate.isEmpty(lst) || i >= lst.size()) {
            return false;
        }
        GenericValue subContentDataResourceView = lst.get(i);
        ctx.put("subContentDataResourceView", subContentDataResourceView);
        GenericValue electronicText = null;
        try {
            electronicText = subContentDataResourceView.getRelatedOne("ElectronicText", false);
        } catch (GenericEntityException e) {
            throw new RuntimeException(e.getMessage());
        }

        String dataResourceId = (String) subContentDataResourceView.get("drDataResourceId");
        String subContentIdSub = (String) subContentDataResourceView.get("contentId"); // in ContentAssocDataResourceViewTo
        // This order is taken so that the dataResourceType can be overridden in the transform arguments.
        String subDataResourceTypeId = (String) ctx.get("subDataResourceTypeId");
        if (UtilValidate.isEmpty(subDataResourceTypeId)) {
            subDataResourceTypeId = (String) subContentDataResourceView.get("drDataResourceTypeId");
            // TODO: If this value is still empty then it is probably necessary to get a value from
            // the parent context. But it will already have one and it is the same context that is
            // being passed.
        }
        // This order is taken so that the mimeType can be overridden in the transform arguments.
        String mimeTypeId = (String) ctx.get("mimeTypeId");
        if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = (String) subContentDataResourceView.get("mimeTypeId");
            String parentContentId = (String) ctx.get("contentId");
            if (UtilValidate.isEmpty(mimeTypeId) && UtilValidate.isNotEmpty(parentContentId)) { // will need these below
                try {
                    GenericValue parentContent = EntityQuery.use(delegator).from("Content").where("contentId", parentContentId).queryOne();
                    if (parentContent != null) {
                        mimeTypeId = (String) parentContent.get("mimeTypeId");
                        ctx.put("parentContent", parentContent);
                    }
                } catch (GenericEntityException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

        }

        // This is what the FM template will see.
        ctx.put("subContentDataResourceView", subContentDataResourceView);
        if (electronicText != null) {
            ctx.put("textData", electronicText.get("textData"));
        } else {
            ctx.put("textData", null);
        }
        ctx.put("content", subContentDataResourceView);
        ctx.put("entityIndex", i + 1);
        ctx.put("subContentId", subContentIdSub);
        ctx.put("drDataResourceId", dataResourceId);
        ctx.put("mimeTypeId", mimeTypeId);
        ctx.put("dataResourceId", dataResourceId);
        ctx.put("subContentIdSub", subContentIdSub);
        ctx.put("subDataResourceTypeId", subDataResourceTypeId);
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final StringBuilder buf = new StringBuilder();
        final Environment env = Environment.getCurrentEnvironment();
        final Map<String, Object> templateCtx = FreeMarkerWorker.getWrappedObject("context", env);
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final Delegator delegator = FreeMarkerWorker.getWrappedObject("delegator", env);
        final Map<String, Object> savedValues = FreeMarkerWorker.saveValues(templateCtx, SAVE_KEY_NAMES);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);

        String contentAssocTypeId = (String) templateCtx.get("contentAssocTypeId");
        if (UtilValidate.isEmpty(contentAssocTypeId)) {
            contentAssocTypeId = "SUB_CONTENT";
            templateCtx.put("contentAssocTypeId ", contentAssocTypeId);
        }

        List<String> assocTypes = UtilMisc.toList(contentAssocTypeId);
        templateCtx.put("assocTypes", assocTypes);
        Locale locale = (Locale) templateCtx.get("locale");
        if (locale == null) {
            locale = Locale.getDefault();
            templateCtx.put("locale", locale);
        }

        String fromDateStr = (String) templateCtx.get("fromDateStr");
        Timestamp fromDate = null;
        if (UtilValidate.isNotEmpty(fromDateStr)) {
            fromDate = UtilDateTime.toTimestamp(fromDateStr);
        }
        if (fromDate == null) {
            fromDate = UtilDateTime.nowTimestamp();
        }

        String thisContentId = (String) templateCtx.get("contentId");

        //DEJ20080730 Should always use contentId, not subContentId since we're searching for that and it is confusing
        String thisMapKey = (String) templateCtx.get("mapKey");
        Map<String, Object> results = ContentServicesComplex.getAssocAndContentAndDataResourceMethod(delegator, thisContentId,
                thisMapKey, null, fromDate, null, null, null, assocTypes, null);
        List<GenericValue> entityList = UtilGenerics.cast(results.get("entityList"));
        templateCtx.put("entityList", entityList);

        return new LoopWriter(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public int onStart() throws TemplateModelException, IOException {
                templateCtx.put("entityIndex", 0);
                boolean inProgress = prepCtx(delegator, templateCtx);
                if (inProgress) {
                    return TransformControl.EVALUATE_BODY;
                }
                return TransformControl.SKIP_BODY;
            }

            @Override
            public int afterBody() throws TemplateModelException, IOException {
                boolean inProgress = prepCtx(delegator, templateCtx);
                if (inProgress) {
                    return TransformControl.REPEAT_EVALUATION;
                }
                return TransformControl.END_EVALUATION;
            }

            @Override
            public void close() throws IOException {
                String wrappedFTL = buf.toString();
                String encloseWrappedText = (String) templateCtx.get("encloseWrappedText");
                if (UtilValidate.isEmpty(encloseWrappedText) || "false".equalsIgnoreCase(encloseWrappedText)) {
                    out.write(wrappedFTL);
                    wrappedFTL = ""; // So it won't get written again below.
                }
                String wrapTemplateId = (String) templateCtx.get("wrapTemplateId");
                if (UtilValidate.isNotEmpty(wrapTemplateId)) {
                    templateCtx.put("wrappedFTL", wrappedFTL);

                    Map<String, Object> templateRoot = FreeMarkerWorker.createEnvironmentMap(env);
                    templateRoot.put("wrapDataResourceTypeId", templateCtx.get("subDataResourceTypeId"));
                    templateRoot.put("wrapContentIdTo", templateCtx.get("contentId"));
                    templateRoot.put("wrapMimeTypeId", templateCtx.get("mimeTypeId"));
                    templateRoot.put("context", templateCtx);

                    Locale locale = (Locale) templateCtx.get("locale");
                    if (locale == null) {
                        locale = Locale.getDefault();
                    }
                    String mimeTypeId = (String) templateCtx.get("mimeTypeId");
                    try {
                        ContentWorker.renderContentAsText(dispatcher, wrapTemplateId, out, templateRoot, locale, mimeTypeId, null, null, true);
                    } catch (GeneralException e) {
                        Debug.logError(e, "Error rendering content", MODULE);
                        throw new IOException("Error rendering content" + e.toString());
                    }
                } else {
                    if (UtilValidate.isNotEmpty(wrappedFTL)) {
                        out.write(wrappedFTL);
                    }
                }
                FreeMarkerWorker.removeValues(templateCtx, REMOVE_KEY_NAMES);
                FreeMarkerWorker.reloadValues(templateCtx, savedValues, env);
            }
        };
    }
}
