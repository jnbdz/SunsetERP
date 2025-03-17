package org.sitenetsoft.sunseterp.applications.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.GeneralException;
import org.sitenetsoft.sunseterp.framework.base.util.UtilHttp;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.base.util.collections.MapStack;
import org.sitenetsoft.sunseterp.framework.base.util.template.FreeMarkerWorker;
import org.sitenetsoft.sunseterp.applications.content.content.ContentWorker;
import org.sitenetsoft.sunseterp.framework.service.LocalDispatcher;

import freemarker.core.Environment;
import freemarker.template.TemplateTransformModel;

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
