package org.sitenetsoft.sunseterp.cli;

/**
 * Utility class for the SunsetERP CLI application.
 */
public final class Util {

    /**
     * Highlights and prints an error message.
     * @param errorMessage The error message to print.
     */
    static void highlightAndPrintErrorMessage(String errorMessage) {
        System.err.println(
                "==============================================================================="
                + System.lineSeparator()
                + errorMessage
                + System.lineSeparator()
                + "===============================================================================");
    }

}
