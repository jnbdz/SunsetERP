Start
========
1. `org/sitenetsoft/sunseterp/framework/start/Start.java`
  - `main(String[] args)`
2. `org/sitenetsoft/sunseterp/framework/start/StartupCommandUtil.java`
  - `parseOfbizCommands(final String[] args)`
  - `getOfbizStartupOptions()` - For the `--help` option for OFBiz commands (HELP, LOAD_DATA, SHUTDOWN, START, STATUS, TEST, PORTOFFSET)
    - `printOfbizStartupHelp(final PrintStream printStream)` - Prints the help message for OFBiz commands
  - `validateAllCommandArguments(CommandLine commandLine)`
  - `mapCommonsCliOptionsToStartupCommands(final CommandLine commandLine)`
3. `org/sitenetsoft/sunseterp/framework/start/Start.java`
  - `CommandType`
    - `valueOf(List<StartupCommand> ofbizCommands)`
4. `org/sitenetsoft/sunseterp/framework/start/StartupControlPanel.java`
  - `init(List<StartupCommand> ofbizCommands)`
  - `loadGlobalOfbizSystemProperties("ofbiz.system.props")`
    - `System.getProperty(globalOfbizPropertiesFileName)` - Is getting the value of the system property `ofbiz.system.props` which is set in ???

## Random notes

Tomcat is one of the modules started as a container.

4. About the DTD of ofbiz-component.xml:
   http://ofbiz.apache.org/dtds/ofbiz-component.xsd


<classpath type="jar" location="lib/*"/> ------------------>classpath related
<classpath type="jar" location="build/lib/*"/>
<classpath type="dir" location="config"/>
<entity-resource type="model"
reader-name="main" loader="main" location="entitydef/entitymodel.xml"/>




Classpath can support these two forms:

<classpath type="jar" location="lib/product.jar"/>
<classpath type="dir" location="classes"/>




5. Configuration file reading process:
   1), first find the global configuration file from System.getProperty("ofbiz.system.props") and load it into System properties.
   2), through String cfgFile = Start.getConfigFileName(firstArg); you get something similar to:
   org/ofbiz/base/start/start.properties
   such configuration file.

6.Loading of classpath:
org.ofbiz.base.start.Start.initClasspath()
org.ofbiz.base.start.Start.loadLibs(String, boolean)
It will add the following path to the classpath:
E:\eclipse-SDK-3.7.1-win32\ofbiz\apache-ofbiz-10.04\ofbiz.jar; ------------->Startup class
E:\jdk1.6.0_20\lib\tools.jar;
E:\eclipse-SDK-3.7.1-win32\ofbiz\apache-ofbiz-10.04 ------------>ofbiz HOME

E:/eclipse-SDK-3.7.1-win32/ofbiz/apache-ofbiz-10.04/framework/base/lib
E:/eclipse-SDK-3.7.1-win32/ofbiz/apache-ofbiz-10.04/framework/base/build/lib/ofbiz-base.jar
E:/eclipse-SDK-3.7.1-win32/ofbiz/apache-ofbiz-10.04/framework/base/dtd
E:/eclipse-SDK-3.7.1-win32/ofbiz/apache-ofbiz-10.04/framework/base/config

and:
containerconfig=E:/eclipse-SDK-3.7.1-win32/ofbiz/apache-ofbiz-10.04/framework/base/config/ofbiz-containers.xml

7. Debug information output: When starting the JVM, add the -DDEBUG=true attribute.
   if (System.getProperty("DEBUG") != null)

8. During startup: