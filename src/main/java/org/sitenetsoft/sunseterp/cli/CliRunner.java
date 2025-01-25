package org.sitenetsoft.sunseterp.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import jakarta.inject.Inject;
import io.quarkus.runtime.QuarkusApplication;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Main entry point for the CLI.
 */
@TopCommand
public class CliRunner implements QuarkusApplication {

    @Inject
    @TopCommand  // Ensure you are using the correct qualifier here
    MainCommand mainCommand;

    /**
     * Main method for the CLI application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        /*System.out.println("Properties");
        System.out.println(System.getProperties());
        System.out.println(System.getenv());*/
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        // TODO: This needs to be work on
        Path path = Paths.get(".", "build").toAbsolutePath().normalize();
        System.setProperty("ofbiz.home", String.valueOf(path));
        // This is needed for Gradle execution
        CommandLine cmd = new CommandLine(new MainCommand());
        System.exit(cmd.execute(args));
    }

    /**
     * Run the CLI.
     * @param args Command-line arguments.
     * @return Exit code.
     */
    @Override
    public int run(String... args) {
        System.out.println("Received arguments: " + Arrays.toString(args));
        CommandLine commandLine = new CommandLine(mainCommand);
        return commandLine.execute(args);
    }
}