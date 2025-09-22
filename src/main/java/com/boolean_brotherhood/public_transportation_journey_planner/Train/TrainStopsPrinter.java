package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.io.IOException;

/**
 * Small utility entry point that loads the train stops and prints each stop using its {@code toString()}.
 */
public final class TrainStopsPrinter {

    private TrainStopsPrinter() {
        // no instances
    }

    public static void main(String[] args) {
        TrainGraph graph = new TrainGraph();
        try {
            graph.loadTrainStops();
        } catch (IOException e) {
            System.err.println("Failed to load train stops: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (graph.getTrainStops().isEmpty()) {
            System.out.println("No train stops were loaded.");
            return;
        }

        System.out.println("Loaded " + graph.getTrainStops().size() + " train stops:\n");
        graph.getTrainStops().forEach(stop -> System.out.println(stop.toString()));
    }
}