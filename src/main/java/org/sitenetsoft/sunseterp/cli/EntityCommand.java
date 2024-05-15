package org.sitenetsoft.sunseterp.cli;

import org.sitenetsoft.sunseterp.framework.start.Start;
import picocli.CommandLine;

import java.nio.file.Paths;

@CommandLine.Command(
        name = "entity",
        mixinStandardHelpOptions = true,
        version = "entity 1.0",
        description = "Entity command"
)
public class EntityCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @CommandLine.Command(name = "list", description = "List entities")
    public void list() {
        System.out.println("List all entities...");
        //System.out.println("ofbizHome: " + ofbizHome);
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        System.out.println(Paths.get(".").toAbsolutePath().normalize());
        System.out.println(System.getProperty("user.dir"));
    }

    @CommandLine.Command(name = "cleanAll", description = "Clean all entities")
    public void cleanAll() {
        System.out.println("Cleaning all entities...");
    }

    @CommandLine.Command(name = "loadAll", description = "Load all entities")
    public void loadAll() {
        System.out.println("Loading all entities...");
        String[] args = {"--load-data"};
        Start.main(args);
    }

    @CommandLine.Command(name = "loadSeed", description = "Load seed entities")
    public void loadSeed() {
        System.out.println("Loading seed entities...");
    }

    @Override
    public void run() {
        // Default action if no subcommand is specified
        System.out.println("No action specified. Please choose a subcommand or use '--help' for options.");
        spec.commandLine().usage(System.out);
    }
}