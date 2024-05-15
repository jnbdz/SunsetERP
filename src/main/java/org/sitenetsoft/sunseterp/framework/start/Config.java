/*
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
 */
package org.sitenetsoft.sunseterp.framework.start;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * OFBiz server parameters needed on system startup and retrieved from
 * one of the properties files in the start component
 */
public final class Config {
    /** The default directory where log files are stored. */
    // "..", "..", "..", "..", "runtime", "logs"
    private static final Path DEFAULT_LOG_DIRECTORY = Paths.get("runtime", "logs");

    private final Path ofbizHome;
    private final Path resourcesPath;
    private final Path ofbizResourcesPath;
    private final InetAddress adminAddress;
    private final String adminKey;
    private final int portOffset;
    private final int adminPort;
    private final List<String> loaders;
    private final Path logDir;
    private final boolean shutdownAfterLoad;
    private final boolean useShutdownHook;

    public Path getLogDir() {
        return logDir;
    }

    public List<String> getLoaders() {
        return loaders;
    }

    public boolean isShutdownAfterLoad() {
        return shutdownAfterLoad;
    }

    public boolean isUseShutdownHook() {
        return useShutdownHook;
    }

    public InetAddress getAdminAddress() {
        return adminAddress;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public int getPortOffset() {
        return portOffset;
    }

    public Path getOfbizHome() {
        return ofbizHome;
    }

    public Path getResourcesPath() {
        return resourcesPath;
    }

    public Path getOfbizResourcesPath() {
        return ofbizResourcesPath;
    }

    /**
     * Constructor
     * @param ofbizCommands The OFBiz commands
     * @throws StartupException If an error was encountered.
     */
    public Config(List<StartupCommand> ofbizCommands) throws StartupException {

        // fetch OFBiz Properties object
        Properties props = getPropertiesFile(ofbizCommands);

        // set this class fields
        ofbizHome = getOfbizHome(getProperty(props, "ofbiz.home", "."));
        resourcesPath = getResourcesPath(getProperty(props, "sunseterp.resources.path", "."));
        System.setProperty("sunseterp.resources.path", resourcesPath.toString());
        ofbizResourcesPath = getOFBizResourcesPath(getProperty(props, "sunseterp.ofbiz.resources.path", "."));
        adminAddress = getAdminAddress(getProperty(props, "ofbiz.admin.host", "127.0.0.1"));
        adminKey = getProperty(props, "ofbiz.admin.key", "NA");
        portOffset = getPortOffsetValue(ofbizCommands, "0");
        adminPort = getAdminPort(props, 0, portOffset);
        loaders = Arrays.asList(getProperty(props, "ofbiz.start.loaders", "").split(","));
        logDir = getAbsolutePath(props, "ofbiz.log.dir", DEFAULT_LOG_DIRECTORY, ofbizHome);
        shutdownAfterLoad = "true".equalsIgnoreCase(getProperty(props, "ofbiz.auto.shutdown", "false"));
        useShutdownHook = "true".equalsIgnoreCase(getProperty(props, "ofbiz.enable.hook", "true"));

        System.out.println("Set OFBIZ_HOME to - " + ofbizHome);

        // set system properties
        System.setProperty("ofbiz.home", ofbizHome.toString());
        System.setProperty("sunseterp.ofbiz.resources.path", ofbizResourcesPath.toString());
        System.setProperty("java.awt.headless", getProperty(props, "java.awt.headless", "true"));
        System.setProperty("derby.system.home", getProperty(props, "derby.system.home", "runtime/data/derby"));

        // set default locale and timezone
        Locale.setDefault(getDefaultLocale(props, "en"));
        TimeZone.setDefault(getDefaultTimeZone(props));
    }

    /**
     *
     * Originally came from org.sitenetsoft.sunseterp.framework.start.StartupControlPanel (loadGlobalOfbizSystemProperties)
     *
     * @param globalOfbizPropertiesFileName The name of the global OFBiz properties file
     * @throws StartupException If an error was encountered.
     */
    public static void loadGlobalOfbizSystemProperties(String globalOfbizPropertiesFileName) throws StartupException {
        String systemProperties = System.getProperty(globalOfbizPropertiesFileName);
        if (systemProperties != null) {
            try (FileInputStream stream = new FileInputStream(systemProperties)) {
                System.getProperties().load(stream);
            } catch (IOException e) {
                throw new StartupException("Couldn't load global system props", e);
            }
        }
    }

    /**
     * Creates a log directory if it does not exist.
     * @param props The properties object
     * @param key The key to the log directory
     * @param defaultValue The default value of the log directory
     * @return The log directory
     */
    private static String getProperty(Properties props, String key, String defaultValue) {
        return Optional.ofNullable(System.getProperty(key))
                .orElse(props.getProperty(key, defaultValue));
    }

    /**
     * Returns the absolute path of the given property.
     * @param homeProp The home property
     * @return The absolute path of the given property
     */
    private static Path getOfbizHome(String homeProp) {
        return Paths.get(homeProp).toAbsolutePath().normalize();
    }

    /**
     * Returns the absolute path of the given property.
     * @param resourcesPath The resources path
     * @return The absolute path of the given property
     */
    private static Path getResourcesPath(String resourcesPath) {
        if (resourcesPath.equals(".")) {
            // If the user did not provide a path, use the default path
            String userDir = System.getProperty("user.dir");
            Path defaultPath = Paths.get(
                    userDir,
                    "..",
                    "..",
                    "..",
                    "resources");
            return defaultPath.toAbsolutePath().normalize();
        } else {
            // If the user provided a path, use it
            return Paths.get(resourcesPath).toAbsolutePath().normalize();
        }
    }

    /**
     * Returns the absolute path of the given property.
     * @param resourcesPath The resources path
     * @return The absolute path of the given property
     */
    private static Path getOFBizResourcesPath(String resourcesPath) {
        if (resourcesPath.equals(".")) {
            // If the user did not provide a path, use the default path
            String userDir = System.getProperty("sunseterp.resources.path");
            Path defaultPath = Paths.get(
                    userDir, "main", "org", "sitenetsoft", "sunseterp");
            return defaultPath.toAbsolutePath().normalize();
        } else {
            // If the user provided a path, use it
            return Paths.get(resourcesPath).toAbsolutePath().normalize();
        }
    }

    /**
     * Returns the absolute path of the given property.
     * @param props The properties object
     * @param key The key to the property
     * @param defaultValue The default value of the property
     * @param ofbizHome The OFBiz home path
     * @return The absolute path of the given property
     */
    private static Path getAbsolutePath(Properties props, String key, Path defaultValue, Path ofbizHome) {
        return Paths.get(getProperty(props, key,
                ofbizHome.resolve(props.getProperty(key, defaultValue.toString())).toString()));
    }

    /**
     * Returns the properties file.
     * @param ofbizCommands The OFBiz commands
     * @return The properties file
     * @throws StartupException If an error was encountered.
     */
    private Properties getPropertiesFile(List<StartupCommand> ofbizCommands) throws StartupException {
        String fileName = determineOfbizPropertiesFileName(ofbizCommands);
        String fullyQualifiedFileName = "org/sitenetsoft/sunseterp/framework/start/config/" + fileName;
        Properties props = new Properties();

        try (InputStream propsStream = getClass().getClassLoader().getResourceAsStream(fullyQualifiedFileName)) {
            props.load(propsStream);
        } catch (IOException e) {
            throw new StartupException(e);
        }

        // TODO: Is this in the OFBiz codebase or did I add it?
        System.out.println("Config.java using configuration file " + fileName);
        return props;
    }

    private static String determineOfbizPropertiesFileName(List<StartupCommand> ofbizCommands) {
        if (ofbizCommands.stream().anyMatch(
                option -> option.getName().equals(StartupCommandUtil.StartupOption.LOAD_DATA.getName()))) {
            return "load-data.properties";
        } else if (ofbizCommands.stream().anyMatch(
                option -> option.getName().equals(StartupCommandUtil.StartupOption.TEST.getName()))) {
            return "test.properties";
        } else {
            return "start.properties";
        }
    }

    private static int getPortOffsetValue(List<StartupCommand> ofbizCommands, String defaultOffset) throws StartupException {
        String extractedPortOffset = ofbizCommands.stream()
                .filter(command -> command.getName().equals(StartupCommandUtil.StartupOption.PORTOFFSET.getName()))
                .findFirst()
                .map(ofbizCommand -> ofbizCommand.getProperties().keySet().iterator().next())
                .orElse(defaultOffset);
        try {
            return Integer.parseInt(extractedPortOffset);
        } catch (NumberFormatException e) {
            throw new StartupException("invalid portoffset number: " + extractedPortOffset, e);
        }
    }

    private static int getAdminPort(Properties props, int defaultAdminPort, int portOffsetValue) {
        String adminPortStr = getProperty(props, "ofbiz.admin.port", String.valueOf(defaultAdminPort));
        try {
            return Integer.parseInt(adminPortStr) + portOffsetValue;
        } catch (NumberFormatException e) {
            System.out.println("Error parsing admin port: " + adminPortStr + " -- " + e.getMessage());
            return defaultAdminPort + portOffsetValue;
        }
    }

    private static InetAddress getAdminAddress(String serverHost) throws StartupException {
        try {
            return InetAddress.getByName(serverHost);
        } catch (UnknownHostException e) {
            throw new StartupException(e);
        }
    }

    private static Locale getDefaultLocale(Properties props, String defaultLocale) {
        String localeString = getProperty(props, "ofbiz.locale.default", defaultLocale);
        String locales[] = localeString.split("_");
        Locale locale = null;
        switch (locales.length) {
        case 1:
            locale = new Locale(locales[0]);
            break;
        case 2:
            locale = new Locale(locales[0], locales[1]);
            break;
        case 3:
            locale = new Locale(locales[0], locales[1], locales[2]);
            break;
        default:
            throw new IllegalArgumentException("The combination of properties, ofbiz.locale.default and defaultLocale is invalid. "
                    + Arrays.toString(locales));
        }
        System.setProperty("user.language", localeString);
        return locale;
    }

    private static TimeZone getDefaultTimeZone(Properties props) {
        String defaultTimezone = getProperty(props, "ofbiz.timeZone.default", TimeZone.getDefault().getID());
        return TimeZone.getTimeZone(defaultTimezone);
    }
}
