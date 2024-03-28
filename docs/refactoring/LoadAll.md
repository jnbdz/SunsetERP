Load All
==================
```mermaid
graph TD
    org/apache/ofbiz/base/component/ComponentLoaderConfig.java -->
    framework/base/config/component-load.xml -->
    framework/component-load.xml -->
    applications/component-load.xml -->
    org/apache/ofbiz/base/component/ComponentConfig.java --> (ofbiz-component.xml) -->
    org/apache/ofbiz/base/component/ComponentResourceHandler.java
```

```mermaid
org/apache/ofbiz/base/container/ComponentContainer.java --> loadComponentsInDirectoryUsingLoadFile --> component-load.xml


org/apache/ofbiz/base/container/ComponentContainer.java --> org/apache/ofbiz/base/component/ComponentConfig.java (ComponentConfigCache)
```

For gettting inquiring on server status or requesting system shutdown, the following classes are used:
```mermaid
org/apache/ofbiz/base/container/AdminServerContainer.java
```

For getting the status of the server, the following classes are used:
```mermaid
org/apache/ofbiz/base/start/Start.java ()
```

```java
/**
 * Returns the server's current state.
 */
public ServerState getCurrentState() {
    return serverState.get();
}
```

org/apache/ofbiz/base/container/ContainerConfig.java - Seems to be processing the container configuration file.


Load Container: 

- `main(String[] args)`
- ```java
/**
* main is the entry point to execute high level OFBiz commands
* such as starting, stopping or checking the status of the server.
* @param args The commands for OFBiz
*/
```
- org/apache/ofbiz/base/start/StartupControlPanel.java
- start()
- loadContainers()
- org/apache/ofbiz/base/container/ContainerLoader.java