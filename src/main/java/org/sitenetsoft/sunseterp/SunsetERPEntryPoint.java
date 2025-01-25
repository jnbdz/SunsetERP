package org.sitenetsoft.sunseterp;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.sitenetsoft.sunseterp.cli.CliRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for the SunsetERP application.
 */
@QuarkusMain
public class SunsetERPEntryPoint implements QuarkusApplication {

    /**
     * Main method for the SunsetERP application.
     * @param args Command-line arguments.
     */
    public static void main(String... args) {
        // TODO: This is called for Quarkus HTTP server and not the CLI... It needs to be checked if needed for the HTTP server
        /*System.out.println("Properties");
        System.out.println(System.getProperties());
        System.out.println(System.getenv());*/
        //System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        Quarkus.run(SunsetERPEntryPoint.class, args);
    }

    /**
     * Run the application.
     * @param args Command-line arguments.
     * @return Exit code.
     */
    @Override
    public int run(String... args) {
        if (args != null && args.length > 0 && !args[0].equals("--debug-jvm")) {
            // Disable HTTP server and OIDC
            System.setProperty("quarkus.http.port", "0");
            System.setProperty("quarkus.oidc.enabled", "false");
            // Execute the CLI through CliRunner
            return new CliRunner().run(args);
        } else {
            // Proceed with normal application startup
            Quarkus.waitForExit();
        }
        return 0;
    }

}
