package org.sitenetsoft.sunseterp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import org.sitenetsoft.sunseterp.framework.start.Start;

/**
 * This class is used to start the application when the Quarkus is starting up.
 * It is an application scoped bean.
 */
@ApplicationScoped
public class Startup {

    /**
     * This method is called when the Quarkus is starting up.
     * It will load the objects (containers (not like Docker containers), services, etc.) and start the application.
     *
     * @param ev The event that is fired when the Quarkus is starting up.
     */
    void onStart(@Observes StartupEvent ev) {
        String[] args = {}; // You can replace this with actual arguments if needed

        // Originally, the main method of the Start class for OFBiz
        Start.main(args);
    }

}