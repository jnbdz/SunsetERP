package org.sitenetsoft.sunseterp.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;
import jakarta.inject.Inject;
import io.quarkus.runtime.QuarkusApplication;

@TopCommand
public class CliRunner implements QuarkusApplication {

    @Inject
    @TopCommand  // Ensure you are using the correct qualifier here
    MainCommand mainCommand;

    @Override
    public int run(String... args) {
        CommandLine commandLine = new CommandLine(mainCommand);
        return commandLine.execute(args);
    }
}