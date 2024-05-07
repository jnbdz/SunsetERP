package org.sitenetsoft.sunseterp;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.sitenetsoft.sunseterp.cli.CliRunner;

@QuarkusMain
public class SunsetERPEntryPoint implements QuarkusApplication {

    public static void main(String... args) {
        Quarkus.run(SunsetERPEntryPoint.class, args);
    }

    @Override
    public int run(String... args) {
        if (args != null && args.length > 0) {
            // Execute the CLI through CliRunner
            return new CliRunner().run(args);
        } else {
            // Proceed with normal application startup
            Quarkus.waitForExit();
        }
        return 0;
    }

}
