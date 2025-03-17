package org.sitenetsoft.sunseterp.framework.webapp.taglib;

import java.io.IOException;

//import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import org.sitenetsoft.sunseterp.framework.base.util.Debug;
import org.sitenetsoft.sunseterp.framework.base.util.UtilMisc;
import org.sitenetsoft.sunseterp.framework.base.util.UtilProperties;
import org.sitenetsoft.sunseterp.framework.base.util.UtilValidate;
import org.sitenetsoft.sunseterp.framework.entity.GenericValue;
import org.sitenetsoft.sunseterp.framework.webapp.website.WebSiteWorker;

/**
 * ContentUrlTag - Creates a URL string prepending the content prefix from url.properties
 */
public class ContentUrlTag {

    private static final String MODULE = ContentUrlTag.class.getName();

    public static void appendContentPrefix(HttpServletRequest request, StringBuilder urlBuffer) {
        try {
            appendContentPrefix(request, (Appendable) urlBuffer);
        } catch (IOException e) {
            throw UtilMisc.initCause(new InternalError(e.getMessage()), e);
        }
    }

    public static void appendContentPrefix(HttpServletRequest request, Appendable urlBuffer) throws IOException {
        if (request == null) {
            Debug.logWarning("Request was null in appendContentPrefix; this probably means this was used where it shouldn't be, like using"
                    + " ofbizContentUrl in a screen rendered through a service; using best-bet behavior: standard prefix from url.properties"
                    + " or secure prefix if no.http is Y  (no WebSite or security setting known)", MODULE);
            String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
            String noHttp = UtilProperties.getPropertyValue("url", "no.http");
            if (noHttp != null && "Y".equals(noHttp)) {
                prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.secure");
            }
            if (prefix != null) {
                urlBuffer.append(prefix.trim());
            }
            return;
        }
        GenericValue webSite = WebSiteWorker.getWebSite(request);
        boolean isForwardedSecure = "HTTPS".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        boolean isSecure = request.isSecure() || isForwardedSecure;
        appendContentPrefix(webSite, isSecure, urlBuffer);
    }

    public static void appendContentPrefix(GenericValue webSite, boolean secure, Appendable urlBuffer) throws IOException {
        if (secure) {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("secureContentPrefix"))) {
                urlBuffer.append(webSite.getString("secureContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.secure");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        } else {
            if (webSite != null && UtilValidate.isNotEmpty(webSite.getString("standardContentPrefix"))) {
                urlBuffer.append(webSite.getString("standardContentPrefix").trim());
            } else {
                String prefix = UtilProperties.getPropertyValue("url", "content.url.prefix.standard");
                if (prefix != null) {
                    urlBuffer.append(prefix.trim());
                }
            }
        }
    }

    public static String getContentPrefix(HttpServletRequest request) {
        StringBuilder buf = new StringBuilder();
        ContentUrlTag.appendContentPrefix(request, buf);
        return buf.toString();
    }
}
