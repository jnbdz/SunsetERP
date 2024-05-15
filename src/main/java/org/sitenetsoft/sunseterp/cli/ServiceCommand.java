package org.sitenetsoft.sunseterp.cli;

import picocli.CommandLine;

@CommandLine.Command(
        name = "service",
        mixinStandardHelpOptions = true,
        version = "service 1.0",
        description = "Service command"
)
public class ServiceCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Command(name = "list", description = "List services")
    public void list() {
        System.out.println("List all services...");
    }

    @CommandLine.Command(name = "cleanAll", description = "Clean all services")
    public void cleanAll() {
        // Implement cleanAll functionality here
        System.out.println("Cleaning all services...");
    }

    @CommandLine.Command(name = "loadAll", description = "Load all services")
    public void loadAll() {
        // Implement loadAll functionality here
        System.out.println("Loading all services...");
    }

    @CommandLine.Command(name = "loadSeed", description = "Load seed services")
    public void loadSeed() {
        // Implement loadSeed functionality here
        System.out.println("Loading seed services...");
    }

    @Override
    public void run() {
        // Default action if no subcommand is specified
        System.out.println("No action specified. Please choose a subcommand or use '--help' for options.");
        spec.commandLine().usage(System.out);
    }

}
