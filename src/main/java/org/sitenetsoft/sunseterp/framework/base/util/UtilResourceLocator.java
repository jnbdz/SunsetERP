package org.sitenetsoft.sunseterp.framework.base.util;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UtilResourceLocator {

    /*private static final String MODULE = UtilURL.class.getName();

    public static URL locateResource(String resourceName) {
        URL url = UtilURL.getUrlMap().get(resourceName);

        if (url != null) {
            return url;
        }

        // Get the calling class
        Class<?> callingClass;
        try {
            callingClass = getCallingClass();
        } catch (IllegalStateException e) {
            Debug.logError(e, MODULE);
            return null;
        }

        // Construct the resource path based on the package structure
        String resourcePath = getResourcePath(callingClass, resourceName);

        // Use the constructed resource path to load the resource
        return UtilURL.fromResource(resourcePath);
    }

    private static Class<?> getCallingClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 1; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            try {
                Class<?> clazz = Class.forName(element.getClassName());
                if (clazz != UtilResourceLocator.class) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                // Ignore and continue
            }
        }
        throw new IllegalStateException("Unable to determine calling class.");
    }

    private static String getResourcePath(Class<?> callingClass, String resourceName) {
        String packageName = callingClass.getPackage().getName();
        String regex = "^(([a-zA-Z\\.]+)(?:framework|applications|plugins)\\.[a-zA-Z]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(packageName);

        String extractedPackage;
        if (matcher.find()) {
            extractedPackage = matcher.group(1);
        } else {
            extractedPackage = packageName;
        }

        String packagePath = extractedPackage.replace(".", "/");
        return packagePath + "/config/" + resourceName;
    }*/

}
