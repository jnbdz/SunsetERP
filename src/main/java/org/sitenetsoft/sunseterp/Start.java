package org.sitenetsoft.sunseterp;

import org.sitenetsoft.sunseterp.framework.base.container.ContainerLoader;
import org.sitenetsoft.sunseterp.framework.start.Config;
import org.sitenetsoft.sunseterp.framework.start.StartupCommand;
import org.sitenetsoft.sunseterp.framework.start.StartupException;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * SunsetERP startup class.
 *
 * <p>
 * This class implements a thread-safe state machine. The design is critical
 * for reliable starting and stopping of the server.
 * </p>
 * <p>
 * The machine's current state and state changes must be encapsulated in this
 * class. Client code may query the current state, but it may not change it.
 * </p>
 * <p>
 * This class uses a singleton pattern to guarantee that only one server instance
 * is running in the VM. Client code retrieves the instance by using the
 * {@code getInstance()} static method.
 * </p>
 */
public final class Start {

    private Config config = null;
    private static final ContainerLoader loader = new ContainerLoader();

    // Singleton, do not change
    private static final Start INSTANCE = new Start();
    private Start() {
    }

    /**
     * Start constructor.
     * - setting high level JVM and OFBiz system properties
     * - creating a Config object holding startup configuration parameters
     */
    public static void init() {
        List<StartupCommand> sunsetERPCommands = null;
        try {
            // TODO: After refactoring, this should be removed
            sunsetERPCommands = getEmptyStartupCommandList();
            // Load global OFBiz system properties pass by command line `-D ofbiz.system.props` (TODO: Double check this)
            Config.loadGlobalOfbizSystemProperties("ofbiz.system.props");
            INSTANCE.config = new Config(sunsetERPCommands);

            System.out.println("System.getProperties()");
            System.out.println(System.getProperties());

            System.out.println("INSTANCE.config.getLogDir().toString()");
            System.out.println(INSTANCE.config.getLogDir().toString());

            createLogDirectoryIfMissing(INSTANCE.config.getLogDir().toString());
            loader.load(INSTANCE.config, sunsetERPCommands);
        } catch (StartupException e) {
            fullyTerminateSystem(e);
        }
    }

    /**
     * Properly exit from the system when a StartupException cannot or
     * should not be handled except by exiting the system.
     * A proper system exit is achieved by:
     * - Printing the stack trace for users to see what happened
     * - Executing the shutdown hooks (if existing) through System.exit
     * - Terminating any lingering threads (if existing) through System.exit
     * - Providing an exit code that is not 0 to signal to the build system
     *   or user of failure to execute.
     * @param e The startup exception that cannot / should not be handled
     *   except by terminating the system
     */
    static void fullyTerminateSystem(StartupException e) {
        // TODO: Check if this works with Quarkus
        e.printStackTrace();
        System.exit(1);
    }

    /**
     * Create the log directory if it is missing.
     * TODO: Maybe add support for when logDirName is null
     * @param logDirName The name of the log directory
     */
    private static void createLogDirectoryIfMissing(String logDirName) {
        File logDir = new File(logDirName);
        if (!logDir.exists()) {
            if (logDir.mkdir()) {
                System.out.println("Created OFBiz log dir [" + logDir.getAbsolutePath() + "]");
            }
        }
    }

    /**
     * TODO: This is just a placeholder for the time being since OFBiz requires this. After refactoring it should be removed.
     * @return An empty list of StartupCommand
     */
    private static List<StartupCommand> getEmptyStartupCommandList() {
        return Collections.emptyList();
    }

    /**
     * Returns the <code>Start</code> instance.
     */
    public static Start getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the server's main configuration.
     */
    public Config getConfig() {
        return this.config;
    }

    /**
     * Updates the server's main configuration.
     * @param c  the new configuration
     */
    public void setConfig(Config c) {
        this.config = c;
    }

}
