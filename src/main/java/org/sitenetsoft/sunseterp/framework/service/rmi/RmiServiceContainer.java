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
package org.sitenetsoft.sunseterp.framework.service.rmi;

//import org.sitenetsoft.sunseterp.framework.start.Start;
//import org.sitenetsoft.sunseterp.framework.start.StartupCommand;


/**
 * RMI Service Engine Container / Dispatcher
 */
public class RmiServiceContainer {}
/*public class RmiServiceContainer implements Container {

    private static final String MODULE = RmiServiceContainer.class.getName();

    private RemoteDispatcherImpl remote = null;
    private String configFile = null;
    private String name = null;
    private String containerName;
    // Container methods

    @Override
    public void init(List<StartupCommand> ofbizCommands, String name, String configFile) {
        this.containerName = name;
        this.configFile = configFile;
    }

    @Override
    public boolean start() throws ContainerException {
        // get the container config
        Configuration cfg = ContainerConfig.getConfiguration(containerName);
        Configuration.Property initialCtxProp = cfg.getProperty("use-initial-context");
        Configuration.Property lookupHostProp = cfg.getProperty("bound-host");
        Configuration.Property lookupPortProp = cfg.getProperty("bound-port");
        Configuration.Property lookupNameProp = cfg.getProperty("bound-name");
        Configuration.Property delegatorProp = cfg.getProperty("delegator-name");
        Configuration.Property clientProp = cfg.getProperty("client-factory");
        Configuration.Property serverProp = cfg.getProperty("server-factory");

        // check the required lookup-name property
        if (lookupNameProp == null || UtilValidate.isEmpty(lookupNameProp.value())) {
            throw new ContainerException("Invalid lookup-name defined in container configuration");
        } else {
            this.name = lookupNameProp.value();
        }

        // check the required delegator-name property
        if (delegatorProp == null || UtilValidate.isEmpty(delegatorProp.value())) {
            throw new ContainerException("Invalid delegator-name defined in container configuration");
        }

        String useCtx = initialCtxProp == null || initialCtxProp.value() == null ? "false" : initialCtxProp.value();
        String host = lookupHostProp == null || lookupHostProp.value() == null ? "localhost" : lookupHostProp.value();
        String port = lookupPortProp == null || lookupPortProp.value() == null ? "1099" : lookupPortProp.value();
        if (Start.getInstance().getConfig().getPortOffset() != 0) {
            Integer portValue = Integer.valueOf(port);
            portValue += Start.getInstance().getConfig().getPortOffset();
            port = portValue.toString();
        }
        String keystore = ContainerConfig.getPropertyValue(cfg, "ssl-keystore", null);
        String ksType = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-type", "JKS");
        String ksPass = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-pass", null);
        String ksAlias = ContainerConfig.getPropertyValue(cfg, "ssl-keystore-alias", null);
        boolean clientAuth = ContainerConfig.getPropertyValue(cfg, "ssl-client-auth", false);

        // setup the factories
        RMIClientSocketFactory csf = null;
        RMIServerSocketFactory ssf = null;

        // get the classloader
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        // load the factories
        if (clientProp != null && UtilValidate.isNotEmpty(clientProp.value())) {
            try {
                Class<?> c = loader.loadClass(clientProp.value());
                csf = (RMIClientSocketFactory) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }
        if (serverProp != null && UtilValidate.isNotEmpty(serverProp.value())) {
            try {
                Class<?> c = loader.loadClass(serverProp.value());
                ssf = (RMIServerSocketFactory) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new ContainerException(e);
            }
        }

        // set the client auth flag on our custom SSL socket factory
        if (ssf != null && ssf instanceof org.sitenetsoft.sunseterp.framework.service.rmi.socket.ssl.SSLServerSocketFactory) {
            // CHECKSTYLE_OFF: ALMOST_ALL
            ((org.sitenetsoft.sunseterp.framework.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setNeedClientAuth(clientAuth);
            ((org.sitenetsoft.sunseterp.framework.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setKeyStoreAlias(ksAlias);
            if (keystore != null) {
                ((org.sitenetsoft.sunseterp.framework.service.rmi.socket.ssl.SSLServerSocketFactory) ssf).setKeyStore(keystore, ksType, ksPass);
            }
            // CHECKSTYLE_ON: ALMOST_ALL
        }

        // get the delegator for this container
        Delegator delegator = DelegatorFactory.getDelegator(delegatorProp.value());

        // create the LocalDispatcher
        LocalDispatcher dispatcher = ServiceContainer.getLocalDispatcher(name, delegator);

        // create the RemoteDispatcher
        try {
            remote = new RemoteDispatcherImpl(dispatcher, csf, ssf);
        } catch (RemoteException e) {
            throw new ContainerException("Unable to start the RMI dispatcher", e);
        }

        if (!"true".equalsIgnoreCase(useCtx)) {
            // bind RMIDispatcher to RMI Naming (Must be JRMP protocol)
            try {
                Naming.rebind("//" + host + ":" + port + "/" + name, remote);
            } catch (RemoteException e) {
                throw new ContainerException("Unable to bind RMIDispatcher to RMI on " + "//host[" + host + "]:port[" + port + "]/name["
                        + name + "] - with remote=" + remote, e);
            } catch (java.net.MalformedURLException e) {
                throw new ContainerException("Invalid URL for binding", e);
            }
        } else {
            // bind RMIDispatcher to InitialContext (must be RMI protocol not IIOP)
            try {
                InitialContext ic = new InitialContext();
                ic.rebind(name, remote);
            } catch (NamingException e) {
                throw new ContainerException("Unable to bind RMIDispatcher to JNDI", e);
            }

            // check JNDI
            try {
                InitialContext ic = new InitialContext();
                Object o = ic.lookup(name);
                if (o == null) {
                    throw new NamingException("Object came back null");
                }
            } catch (NamingException e) {
                throw new ContainerException("Unable to lookup bound objects", e);
            }
        }

        return true;
    }

    @Override
    public void stop() {
        remote.deregister();
    }

    @Override
    public String getName() {
        return containerName;
    }
}*/
