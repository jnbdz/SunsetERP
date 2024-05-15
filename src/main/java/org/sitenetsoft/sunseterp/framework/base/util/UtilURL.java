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
package org.sitenetsoft.sunseterp.framework.base.util;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * URL Utilities - Simple Class for flexibly working with properties files
 *
 */
public final class UtilURL {

    private static final String MODULE = UtilURL.class.getName();
    private static final Map<String, URL> URL_MAP = new ConcurrentHashMap<>();

    private UtilURL() { }

    public static <C> URL fromClass(Class<C> contextClass) {
        String resourceName = contextClass.getName();
        int dotIndex = resourceName.lastIndexOf('.');

        if (dotIndex != -1) {
            resourceName = resourceName.substring(0, dotIndex);
        }
        resourceName += ".properties";

        return fromResource(contextClass, resourceName);
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.</p>
     * @param resourceName
     * @return
     */
    public static URL fromResource(String resourceName) {
        return fromResource(resourceName, null);
    }

    public static <C> URL fromResource(Class<C> contextClass, String resourceName) {
        if (contextClass == null) {
            return fromResource(resourceName, null);
        }
        return fromResource(resourceName, contextClass.getClassLoader());
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.</p>
     * @param resourceName
     * @param loader
     * @return
     */
    public static URL fromResource(String resourceName, ClassLoader loader) {
        URL url = URL_MAP.get(resourceName);
        URI uri;
        if (url != null) {
            try {
                uri = new URI(url.toString());
                url = uri.toURL();
            } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
                Debug.logWarning(e, "Exception thrown while copying URL", MODULE);
            }
        }
        if (loader == null) {
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                // Huh? The new object will be created by the current thread, so how is this any different than the previous code?
                loader = UtilURL.class.getClassLoader();
            }
        }

        // TODO: Have someone review why the CLASSPATH is not working as expected
        url = loader.getResource(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromResourcesPath(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        System.out.println("URL not found: " + resourceName);
        System.out.println("URL: " + url);
        url = ClassLoader.getSystemResource(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromFilename(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromOfbizHomePath(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromUrlString(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
        }
        return url;
    }

    // TODO: Remove
    /*public static void listClasspath() {
        // Get the classpath string from the system properties
        String classpath = System.getProperty("java.class.path");
        // Get the path separator used on the current operating system
        String pathSeparator = File.pathSeparator;

        // Split the classpath string into individual paths
        String[] classpathEntries = classpath.split(pathSeparator);

        // Print each entry in the classpath
        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        System.out.println("Classpath entries:");
        for (String entry : classpathEntries) {
            System.out.println(entry);
            //listJarContents(fromFilename(entry));
        }
        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
    }

    // TODO: Remove
    public static void listJarContents(URL jarUrl) {
        try {
            // Convert URL to a JarFile
            JarFile jar = new JarFile(jarUrl.toURI().getPath());

            System.out.println("Contents of the JAR:");
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                System.out.println(entry.getName());
            }
            jar.close();
        } catch (IOException | URISyntaxException e) {
            System.err.println("Error accessing JAR: " + e.getMessage());
        }
    }*/

    public static URL fromFilename(String filename) {
        if (filename == null) {
            return null;
        }
        File file = new File(filename);
        URL url = null;

        try {
            if (file.exists()) {
                url = file.toURI().toURL();
            }
        } catch (MalformedURLException e) {
            Debug.logError(e, "unable to retrieve URL for file: " + filename, MODULE);
        }
        return url;
    }

    public static URL fromUrlString(String urlString) {
        URL url = null;
        URI uri;
        try {
            uri = new URI(urlString);
            url = uri.toURL();
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            // We purposely don't want to do anything here.
        }
        return url;
    }

    public static URL fromOfbizHomePath(String filename) {
        String ofbizHome = System.getProperty("ofbiz.home");
        System.out.println("ofbizHome: " + ofbizHome);
        if (ofbizHome == null) {
            Debug.logWarning("No ofbiz.home property set in environment", MODULE);
            return null;
        }
        String newFilename = ofbizHome;
        if (!newFilename.endsWith("/") && !filename.startsWith("/")) {
            newFilename = newFilename + "/";
        }
        newFilename = newFilename + filename;
        return fromFilename(newFilename);
    }

    /*public static URL fromResourcesPath(String filename) {
        String ofbizHome = System.getProperty("ofbiz.home");

        if (ofbizHome == null) {
            Debug.logWarning("No ofbiz.home property set in environment", MODULE);
            return null;
        }

        // Define the root directories to search
        String[] rootDirs = {"applications", "framework"};

        // Convert the forEach to a stream and use findFirst to short-circuit and return when the first match is found
        for (String rootDir : rootDirs) {
            Path basePath = Paths.get(ofbizHome, "..", "..", "..", "resources", "main", "org", "sitenetsoft", "sunseterp", rootDir);

            try {
                Optional<URL> foundUrl = Files.walk(basePath, 1) // Use 1 to limit depth to immediate subdirectories
                        .filter(Files::isDirectory) // Ensure it is a directory
                        .map(dir -> dir.resolve("config"))
                        .filter(Files::isDirectory)
                        .map(configPath -> configPath.resolve(filename))
                        .filter(Files::exists)
                        .map(filePath -> {
                            try {
                                System.out.println("Found URL: " + filePath.toUri().toURL());
                                return filePath.toUri().toURL();
                            } catch (MalformedURLException e) {
                                Debug.logError(e, "Failed to convert file path to URL", MODULE);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst();

                if (foundUrl.isPresent()) {
                    return foundUrl.get();
                }
            } catch (IOException e) {
                Debug.logError(e, "Error walking through directory " + basePath, MODULE);
            }
        }

        return null; // Return null if the file isn't found after all attempts
    }*/

    public static URL fromResourcesPath(String filename) {
        String ofbizHome = System.getProperty("ofbiz.home");
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("ofbizHome: " + ofbizHome);
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        System.out.println("-----------------------");
        System.out.println("-----------------------");
        System.out.println("-----------------------");


        if (ofbizHome == null) {
            Debug.logWarning("No ofbiz.home property set in environment", MODULE);
            return null;
        }

        // Define the root directories to search
        String[] rootDirs = {"applications", "framework"};

        // Iterate over root directories and search in all subdirectories
        for (String rootDir : rootDirs) {
            Path basePath = Paths.get(
                    ofbizHome, "..", "..", "..", "resources", "main", "org", "sitenetsoft", "sunseterp", rootDir);

            try {
                Optional<URL> foundUrl = Files.walk(basePath, 1) // Use 1 to limit depth to immediate subdirectories
                        .filter(Files::isDirectory) // Ensure it is a directory
                        .flatMap(dir -> {
                            try {
                                return Files.list(dir); // List all directories at the same level
                            } catch (IOException e) {
                                Debug.logError(e, "Error listing directories in " + dir, MODULE);
                                return Stream.empty(); // In case of an error, return an empty stream
                            }
                        })
                        .filter(Files::isDirectory) // Ensure it is a directory
                        .map(subDir -> subDir.resolve(filename))
                        .filter(Files::exists)
                        .map(filePath -> {
                            try {
                                System.out.println("Found URL: " + filePath.toUri().toURL());
                                return filePath.toUri().toURL();
                            } catch (MalformedURLException e) {
                                Debug.logError(e, "Failed to convert file path to URL", MODULE);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst();

                if (foundUrl.isPresent()) {
                    return foundUrl.get();
                }
            } catch (IOException e) {
                Debug.logError(e, "Error walking through directory " + basePath, MODULE);
            }
        }

        return null; // Return null if the file isn't found after all attempts
    }

    public static String getOfbizHomeRelativeLocation(URL fileUrl) {
        String ofbizHome = System.getProperty("ofbiz.home");
        String path = fileUrl.getPath();
        if (path.startsWith(ofbizHome)) {
            // note: the +1 is to remove the leading slash
            path = path.substring(ofbizHome.length() + 1);
        }
        return path;
    }

}
