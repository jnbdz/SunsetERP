package org.sitenetsoft.sunseterp.cli;

import picocli.CommandLine;

@CommandLine.Command(name = "entity", mixinStandardHelpOptions = true, version = "entity 1.0",
        description = "Entity command")
public class EntityCommand {

    @CommandLine.Command(name = "list", description = "List entities")
    public void list() {
        System.out.println("List all entities...");
    }

    @CommandLine.Command(name = "cleanAll", description = "Clean all entities")
    public void cleanAll() {
        // Implement cleanAll functionality here
        System.out.println("Cleaning all entities...");
    }

    @CommandLine.Command(name = "loadAll", description = "Load all entities")
    public void loadAll() {
        // Implement loadAll functionality here
        System.out.println("Loading all entities...");
    }

    @CommandLine.Command(name = "loadSeed", description = "Load seed entities")
    public void loadSeed() {
        // Implement loadSeed functionality here
        System.out.println("Loading seed entities...");
    }
}