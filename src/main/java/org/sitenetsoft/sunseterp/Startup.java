package org.sitenetsoft.sunseterp;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.CommandLineArguments;
import org.sitenetsoft.sunseterp.cli.MainCommand;
import org.sitenetsoft.sunseterp.framework.start.Start;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

/**
 * This class is used to start the application when Quarkus is starting up.
 * It is an application scoped bean.
 */
@ApplicationScoped
public class Startup {

    @Inject
    CommandLine.IFactory factory;

    // Inject MainCommand with the correct qualifier
    @Inject
    @TopCommand
    MainCommand mainCommand;

    @CommandLineArguments
    String[] args;

    private static final Set<String> CLI_ACTIONS = new HashSet<>(Arrays.asList("controller", "entity", "service"));

    /**
     * This method is called when Quarkus is starting up.
     * It decides whether to load Picocli based on the presence of certain arguments.
     *
     * @param ev The event that is fired when Quarkus is starting up.
     */
    void onStart(@Observes StartupEvent ev) {
        Path pathOne = Paths.get(".", "..", "..", "..").toAbsolutePath().normalize();
        System.setProperty("user.dir", String.valueOf(pathOne));
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println(System.getProperty("user.dir"));
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        if (isCliMode(args)) {
            // Run as CLI
            new CommandLine(mainCommand, factory).execute(args);
        } else {
            // Normal application startup
            Path path = Paths.get(".", "..", "..", "..").toAbsolutePath().normalize();
            System.setProperty("user.dir", String.valueOf(path));
            Start.main(args);
        }
    }

    /**
     * Determines if the application should run in CLI mode.
     *
     * @param args the command line arguments passed to the application
     * @return true if any CLI action is detected, false otherwise
     */
    private boolean isCliMode(String[] args) {
        for (String arg : args) {
            if (CLI_ACTIONS.contains(arg)) {
                return true;
            }
        }
        return false;
    }
}
