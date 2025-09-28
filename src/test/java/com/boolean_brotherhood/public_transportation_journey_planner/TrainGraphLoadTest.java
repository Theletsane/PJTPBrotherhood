package com.boolean_brotherhood.public_transportation_journey_planner;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;

class TrainGraphTests {

    private TrainGraph trainGraph;

    @BeforeEach
    void setUp() throws IOException {
        trainGraph = new TrainGraph();
        trainGraph.loadTrainStops(); // always load stops before tests
    }

    @Test
    void testLoadTrainStops() {
        List<TrainStop> stops = trainGraph.getTrainStops();

        assertNotNull(stops, "Train stops list should not be null");
        assertFalse(stops.isEmpty(), "Train stops list should not be empty");

        // Check known station
        TrainStop capeTown = trainGraph.getStopByName("CAPE TOWN");
        assertNotNull(capeTown, "CAPE TOWN station should exist in the loaded stops");

        // Latitude/longitude sanity
        assertTrue(capeTown.getLatitude() < 0, "Latitude should be negative for Cape Town");
        assertTrue(capeTown.getLongitude() > 0, "Longitude should be positive for Cape Town");
    }

    @Test
    void testGetStopByNameCaseInsensitive() {
        TrainStop stop1 = trainGraph.getStopByName("Cape Town");
        TrainStop stop2 = trainGraph.getStopByName("cape town");

        assertNotNull(stop1, "Stop lookup should work with mixed case");
        assertEquals(stop1, stop2, "Stop lookup should be case-insensitive");
    }

    @Test
    void testGetNearestTrainStop() {
        // Coordinates close to Cape Town central station
        TrainStop nearest = trainGraph.getNearestTrainStop(-33.9258, 18.4232);
        assertNotNull(nearest, "Nearest stop should not be null");
        assertEquals("CAPE TOWN", nearest.getName(), "Expected CAPE TOWN as nearest stop");
    }

}
