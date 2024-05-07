package org.sitenetsoft.sunseterp.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import jakarta.enterprise.context.ApplicationScoped;

@TopCommand
@ApplicationScoped
public class MainCommand implements Runnable {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public void run() {
        // Default action if no command is specified
        spec.commandLine().usage(System.out);
    }
}