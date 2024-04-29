package org.sitenetsoft.sunseterp;

import picocli.CommandLine;

@CommandLine.Command(name = "service", mixinStandardHelpOptions = true, version = "service 1.0",
        description = "Service command")
public class ServiceCommand {
        @CommandLine.Command(name = "list", description = "List services")
        public void list() {
            System.out.println("List all entities...");
        }

        @CommandLine.Command(name = "cleanAll", description = "Clean all services")
        public void cleanAll() {
            // Implement cleanAll functionality here
            System.out.println("Cleaning all entities...");
        }

        @CommandLine.Command(name = "loadAll", description = "Load all services")
        public void loadAll() {
            // Implement loadAll functionality here
            System.out.println("Loading all entities...");
        }

        @CommandLine.Command(name = "loadSeed", description = "Load seed services")
        public void loadSeed() {
            // Implement loadSeed functionality here
            System.out.println("Loading seed entities...");
        }
}
