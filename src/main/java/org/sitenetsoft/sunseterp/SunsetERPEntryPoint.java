package org.sitenetsoft.sunseterp;

import io.quarkus.runtime.Quarkus;
import org.sitenetsoft.sunseterp.framework.start.Start;

public class SunsetERPEntryPoint {

    public static void main(String[] args) {
        // You can perform any setup or initialization here if needed
        // ...

        Start.main(args);
        System.out.println("===============______________________===================");
        System.out.println("SunsetERPEntryPoint.main()");
        System.out.println("===============______________________===================");

        // Start the Quarkus application
        Quarkus.run(args);
    }

}
