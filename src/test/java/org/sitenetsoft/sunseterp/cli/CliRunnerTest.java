package org.sitenetsoft.sunseterp.cli;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

class CliRunnerTest {

    @Test
    void testRun() {
        // Mock CommandLine
        CommandLine commandLineMock = Mockito.mock(CommandLine.class);

        // Stub addSubcommand method
        Mockito.doNothing().when(commandLineMock).addSubcommand(Mockito.anyString(), Mockito.any(Class.class));

        // Set up subcommands map
        Map<String, Class<?>> subcommands = new HashMap<>();
        subcommands.put("entity", EntityCommand.class);
        subcommands.put("controller", ControllerCommand.class);
        subcommands.put("service", ServiceCommand.class);

        // Create Main instance
        CliRunner runner = new CliRunner();

        // Test main method
        runner.run(new String[0]);

        // Verify addSubcommand method is called for each subcommand
        for (Map.Entry<String, Class<?>> entry : subcommands.entrySet()) {
            Mockito.verify(commandLineMock).addSubcommand(entry.getKey(), entry.getValue());
        }
    }
}