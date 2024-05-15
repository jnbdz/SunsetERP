package org.sitenetsoft.sunseterp.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "controller",
        mixinStandardHelpOptions = true,
        version = "controller 1.0",
        description = "Controller command"
)
public class ControllerCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Command(name = "list", description = "List controllers")
    public void list() {
        System.out.println("List all controllers...");
    }

    @CommandLine.Command(name = "cleanAll", description = "Clean all controllers")
    public void cleanAll() {
        // Implement cleanAll functionality here
        System.out.println("Cleaning all controllers...");
    }

    @CommandLine.Command(name = "loadAll", description = "Load all controllers")
    public void loadAll() {
        // Implement loadAll functionality here
        System.out.println("Loading all controllers...");
    }

    @Override
    public void run() {
        // Default action if no subcommand is specified
        System.out.println("No action specified. Please choose a subcommand or use '--help' for options.");
        spec.commandLine().usage(System.out);
    }

}
