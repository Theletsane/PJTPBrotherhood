package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyFileLoader {

    private static final Logger LOGGER = Logger.getLogger(MyFileLoader.class.getName());

    /** Wrap an InputStream in a BufferedReader */
    public static BufferedReader getBufferedReaderOfFile(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /** Wrap a Reader in a BufferedReader */
    public static BufferedReader getBufferedReaderOfFile(Reader reader) {
        return new BufferedReader(reader);
    }

    /**
     * Load a file from the resources folder.
     * Example usage: MyFileLoader.getBufferedReaderFromResource("CapeTownTransitData/complete_metrorail_stations.csv");
     */
    public static BufferedReader getBufferedReaderFromResource(String resourcePath) {
        InputStream inputStream = MyFileLoader.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            LOGGER.log(Level.SEVERE, "Resource not found: {0}", resourcePath);
            return null; // caller must check
        }

        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * Check if a resource exists in the resources folder.
     * Logs a SEVERE message if the resource does not exist.
     */
    public static boolean resourceExists(String resourcePath) {
        InputStream inputStream = MyFileLoader.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            LOGGER.log(Level.SEVERE, "Resource not found: {0}", resourcePath);
            return false;
        }

        try {
            inputStream.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to close input stream for resource: {0}", resourcePath);
        }

        return true;
    }
}
