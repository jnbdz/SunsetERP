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
package org.sitenetsoft.sunseterp.framework.base.container;

import org.sitenetsoft.sunseterp.framework.base.component.ComponentConfig;
import org.sitenetsoft.sunseterp.framework.base.component.ComponentException;
import org.sitenetsoft.sunseterp.framework.base.component.ComponentLoaderConfig;
import org.sitenetsoft.sunseterp.framework.base.component.ComponentLoaderConfig.ComponentDef;
import org.sitenetsoft.sunseterp.framework.start.Start;
import org.sitenetsoft.sunseterp.framework.start.StartupCommand;
import org.sitenetsoft.sunseterp.framework.base.util.Debug;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * ComponentContainer - StartupContainer implementation for Components
 *
 * The purpose of this container is to load the classpath for all components
 * defined in OFBiz. This container must run before any other containers to
 * allow components to access any necessary resources. Furthermore, the
 * ComponentContainer also builds up the <code>ComponentConfigCache</code>
 * defined in <code>ComponentConfig</code> to keep track of loaded components
 *
 */
public class ComponentContainer implements Container {

    private static final String MODULE = ComponentContainer.class.getName();

    private String name;
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) throws ContainerException {
        init(name, Start.getInstance().getConfig().getOfbizHome());
    }

    /**
     * Loads components found in a directory.
     * @param name  the name of this container
     * @param ofbizHome  the directory where to search for components
     * @throws ContainerException when components are already loaded or when failing to load them.
     */
    void init(String name, Path ofbizHome) throws ContainerException {
        if (!loaded.compareAndSet(false, true)) {
            throw new ContainerException("Components already loaded, cannot start");
        }
        this.name = name;

        // TODO: Remove
        /*try {
            System.out.println("init()");
            for (ComponentDef def: ComponentLoaderConfig.getRootComponents()) {
                System.out.println("Loading component: " + def.getLocation());
                System.out.println("Component type: " + def.getType());
                System.out.println("Component toString: " + def.toString());
                System.out.println("ofbizHome: " + ofbizHome);
            }
        } catch (ComponentException e) {
            throw new ContainerException(e);
        }*/

        // load the components from framework/base/config/component-load.xml (root components)
        try {
            for (ComponentDef def: ComponentLoaderConfig.getRootComponents()) {
                if (def.getLocation().toString().contains("plugins")) {
                    // TODO: setup the plugins directory
                    //loadComponent(ofbizHome, def);
                    System.out.println("Trying to load plugins");
                    System.out.println("Loading component: " + def.getLocation());
                } else {
                    loadComponent(Paths.get(
                            String.valueOf(ofbizHome),
                            "..", "..", "..", "resources", "main", "org", "sitenetsoft", "sunseterp"), def);
                }
            }
            System.out.println("--- Sorting dependencies ---");
            ComponentConfig.sortDependencies();
            System.out.println("--- DONE! --- Sorting dependencies ---");
        } catch (IOException | ComponentException e) {
            throw new ContainerException(e);
        }
        System.out.println("All components loaded");
        Debug.logInfo("All components loaded", MODULE);
        System.out.println("All --- Done --- components loaded");
    }

    @Override
    public boolean start() {
        return loaded.get();
    }

    /**
     * Loads any kind of component definition.
     * @param dir  the location where the component should be loaded
     * @param component  a single component or a component directory definition
     * @throws IOException when component directory loading fails.
     */
    private void loadComponent(Path dir, ComponentDef component) throws IOException {
        Path location = component.getLocation().isAbsolute() ? component.getLocation() : dir.resolve(component.getLocation());
        System.out.println("Loading component location: " + location);
        System.out.println("Loading component type: " + component.getType());

        switch (component.getType()) {
        case COMPONENT_DIRECTORY:
            loadComponentDirectory(location);
            break;
        case SINGLE_COMPONENT:
            retrieveComponentConfig(location);
            break;
        }
    }

    /**
     * Checks to see if the directory contains a load file (component-load.xml) and
     * then delegates loading to the appropriate method
     * @param directoryName the name of component directory to load
     * @throws IOException
     */
    private void loadComponentDirectory(Path directoryName) throws IOException {
        Debug.logInfo("Auto-Loading component directory : [" + directoryName + "]", MODULE);
        if (Files.exists(directoryName) && Files.isDirectory(directoryName)) {
            Path componentLoad = directoryName.resolve(ComponentLoaderConfig.COMPONENT_LOAD_XML_FILENAME);

            // TODO
            System.out.println("loadComponentDirectory - componentLoad: " + componentLoad);

            if (Files.exists(componentLoad)) {
                System.out.println("loadComponentDirectory - componentLoad exists");
                loadComponentsInDirectoryUsingLoadFile(directoryName, componentLoad);
            } else {
                System.out.println("loadComponentDirectory - componentLoad does **not** exist");
                loadComponentsInDirectory(directoryName);
            }
        } else {
            Debug.logError("Auto-Load Component directory not found : " + directoryName, MODULE);
        }

    }

    /**
     * load components residing in a directory only if they exist in the component
     * load file (component-load.xml) and they are sorted in order from top to bottom
     * in the load file
     * @param directoryPath the absolute path of the directory
     * @param componentLoadFile the name of the load file (i.e. component-load.xml)
     * @throws IOException
     */
    private void loadComponentsInDirectoryUsingLoadFile(Path directoryPath, Path componentLoadFile) throws IOException {
        URL configUrl = null;
        try {
            configUrl = componentLoadFile.toUri().toURL();
            List<ComponentDef> componentsToLoad = ComponentLoaderConfig.getComponentsFromConfig(configUrl);
            for (ComponentDef def: componentsToLoad) {
                loadComponent(directoryPath, def);
            }
        } catch (MalformedURLException e) {
            Debug.logError(e, "Unable to locate URL for component loading file: " + componentLoadFile.toAbsolutePath(), MODULE);
        } catch (ComponentException e) {
            Debug.logError(e, "Unable to load components from URL: " + configUrl.toExternalForm(), MODULE);
        }
    }

    /**
     * Load all components in a directory because it does not contain
     * a load-components.xml file. The components are sorted alphabetically
     * for loading purposes
     * @param directoryPath a valid absolute path of a component directory
     * @throws IOException if an I/O error occurs when opening the directory
     */
    private static void loadComponentsInDirectory(Path directoryPath) throws IOException {
        try (Stream<Path> paths = Files.list(directoryPath)) {
            paths.sorted()
                    .map(cmpnt -> directoryPath.resolve(cmpnt).toAbsolutePath().normalize())
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve(ComponentConfig.OFBIZ_COMPONENT_XML_FILENAME)))
                    .forEach(ComponentContainer::retrieveComponentConfig);
        }
    }

    /**
     * Fetch the <code>ComponentConfig</code> for a certain component
     * @param location directory location of the component which cannot be {@code null}
     * @return The component configuration
     */
    private static ComponentConfig retrieveComponentConfig(Path location) {
        ComponentConfig config = null;
        try {
            config = ComponentConfig.getComponentConfig(null, location.toString());
        } catch (ComponentException e) {
            Debug.logError("Cannot load component: " + location + " : " + e.getMessage(), MODULE);
        }
        if (config == null) {
            Debug.logError("Cannot load component: " + location, MODULE);
        }
        return config;
    }

    @Override
    public void stop() {
    }

    @Override
    public String getName() {
        return name;
    }
}
