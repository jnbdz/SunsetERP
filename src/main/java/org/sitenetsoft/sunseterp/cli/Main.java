package org.sitenetsoft.sunseterp.cli;

import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

/**
 * Main class for the SunsetERP CLI application.
 */
@CommandLine.Command(name = "SunsetERP", mixinStandardHelpOptions = true, version = "1.0",
        description = "SunsetERP Command Line Interface")
public class Main {

    // Map of subcommands
    private static final Map<String, Class<?>> subcommands = new HashMap<>();

    // Initialize the subcommands
    static {
        subcommands.put("entity", EntityCommand.class);
        subcommands.put("controller", ControllerCommand.class);
        subcommands.put("service", ServiceCommand.class);
    }

    /**
     * Main method for the CLI application.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Main());

        for (Map.Entry<String, Class<?>> entry : subcommands.entrySet()) {
            commandLine.addSubcommand(entry.getKey(), entry.getValue());
        }

        commandLine.execute(args);
    }
}