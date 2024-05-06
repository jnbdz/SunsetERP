package org.sitenetsoft.sunseterp.cli;

import picocli.CommandLine;
import io.quarkus.picocli.runtime.annotations.TopCommand;

import java.util.HashMap;
import java.util.Map;

@TopCommand
@CommandLine.Command(name = "SunsetERP", mixinStandardHelpOptions = true, version = "1.0",
        description = "SunsetERP Command Line Interface")
public class Main implements Runnable {

    // Map of subcommands
    private static final Map<String, Class<?>> subcommands = new HashMap<>();

    // Initialize the subcommands
    static {
        subcommands.put("entity", EntityCommand.class);
        subcommands.put("controller", ControllerCommand.class);
        subcommands.put("service", ServiceCommand.class);
    }

    @Override
    public void run() {
        // Typically you might handle some default action here if no subcommands are specified
        // For now, it does nothing, allowing Picocli to handle help or version commands
    }

    /**
     * Main method for the CLI application.
     * @param args Command-line arguments.
     */
    public static void init(String[] args) {
        CommandLine commandLine = new CommandLine(new Main());

        for (Map.Entry<String, Class<?>> entry : subcommands.entrySet()) {
            commandLine.addSubcommand(entry.getKey(), entry.getValue());
        }

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}