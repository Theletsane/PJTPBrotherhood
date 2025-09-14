package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class MyFileLoader {

    /**
     * Wrap an InputStream in a BufferedReader
     */
    public static BufferedReader getBufferedReaderOfFile(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    /**
     * Wrap a Reader in a BufferedReader
     */
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
            throw new IllegalArgumentException("Resource not found: " + resourcePath);
        }

        return new BufferedReader(new InputStreamReader(inputStream));
    }
}
