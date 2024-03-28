package org.sitenetsoft.sunseterp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import org.sitenetsoft.sunseterp.framework.start.Start;

@ApplicationScoped
public class Startup {

    void onStart(@Observes StartupEvent ev) {
        String[] args = {}; // You can replace this with actual arguments if needed
        Start.main(args);
    }
}