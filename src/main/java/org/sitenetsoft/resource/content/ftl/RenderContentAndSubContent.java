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
package org.sitenetsoft.resource.content.ftl;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;
import org.sitenetsoft.framework.base.util.*;
import org.sitenetsoft.framework.base.util.collections.MapStack;
import org.sitenetsoft.framework.base.util.template.FreeMarkerWorker;
import org.sitenetsoft.resource.content.content.ContentWorker;
import org.sitenetsoft.framework.service.LocalDispatcher;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

/**
 * RenderContentAndSubContent - Freemarker Transform for Content rendering
 * This transform cannot be called recursively (at this time).
 */
public class RenderContentAndSubContent implements TemplateTransformModel {

    private static final String MODULE = RenderContentAndSubContent.class.getName();

    @Override
    @SuppressWarnings("unchecked")
    public Writer getWriter(Writer out, @SuppressWarnings("rawtypes") Map args) {
        final Environment env = Environment.getCurrentEnvironment();
        final LocalDispatcher dispatcher = FreeMarkerWorker.getWrappedObject("dispatcher", env);
        final HttpServletRequest request = FreeMarkerWorker.getWrappedObject("request", env);
        final Map<String, Object> envMap = FreeMarkerWorker.createEnvironmentMap(env);
        final MapStack<String> templateRoot = MapStack.create();
        templateRoot.push(envMap);
        if (Debug.verboseOn()) {
            Debug.logVerbose("in RenderContentAndSubContent, contentId(0):" + templateRoot.get("contentId"), MODULE);
        }
        FreeMarkerWorker.getSiteParameters(request, templateRoot);
        FreeMarkerWorker.overrideWithArgs(templateRoot, args);

        return new Writer(out) {

            @Override
            public void write(char cbuf[], int off, int len) {
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                renderSubContent();
            }

            public void renderSubContent() throws IOException {
                String mimeTypeId = (String) templateRoot.get("mimeTypeId");
                Object localeObject = templateRoot.get("locale");
                Locale locale = null;
                if (localeObject == null) {
                    locale = UtilHttp.getLocale(request);
                } else {
                    locale = UtilMisc.ensureLocale(localeObject);
                }

                if (Debug.verboseOn()) {
                    Debug.logVerbose("in RenderContentAndSubContent, contentId(2):" + templateRoot.get("contentId"), MODULE);
                }
                if (Debug.verboseOn()) {
                    Debug.logVerbose("in RenderContentAndSubContent, subContentId(2):" + templateRoot.get("subContentId"), MODULE);
                }
                    try {
                        String contentId = (String) templateRoot.get("contentId");
                        String mapKey = (String) templateRoot.get("mapKey");
                        String contentAssocTypeId = (String) templateRoot.get("contentAssocTypeId");
                        if (UtilValidate.isNotEmpty(mapKey) || UtilValidate.isNotEmpty(contentAssocTypeId)) {
                            String txt = ContentWorker.renderSubContentAsText(dispatcher, contentId, mapKey, templateRoot, locale, mimeTypeId, true);
                            out.write(txt);
                        } else if (contentId != null) {
                            ContentWorker.renderContentAsText(dispatcher, contentId, out, templateRoot, locale, mimeTypeId, null, null, true);
                        }
                    } catch (GeneralException e) {
                        String errMsg = "Error rendering thisContentId:" + (String) templateRoot.get("contentId") + " msg:" + e.toString();
                        Debug.logError(e, errMsg, MODULE);
                        // just log a message and don't return anything: throw new IOException();
                    }
            }

        };
    }
}
