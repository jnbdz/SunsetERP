package org.sitenetsoft.sunseterp.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import jakarta.enterprise.context.ApplicationScoped;

@TopCommand
@CommandLine.Command(
        name = "main",
        description = "The main entry point for CLI commands.",
        mixinStandardHelpOptions = true, // Enable standard help options
        subcommands = {
                EntityCommand.class,
                ControllerCommand.class,
                ServiceCommand.class
        }
)
public class MainCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        // Default action if no command is specified
        spec.commandLine().usage(System.out);
    }
}