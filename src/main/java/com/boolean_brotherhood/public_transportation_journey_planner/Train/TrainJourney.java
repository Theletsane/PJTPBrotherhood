package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a journey from source to destination, 
 * including stops, trips, and transfer info.
 */
public class TrainJourney {

    private final TrainStop source;
    private final TrainStop destination;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final List<TrainTrips> trips; // ordered sequence of trips

    public TrainJourney(TrainStop source, TrainStop destination, LocalTime departureTime,LocalTime arrivalTime, List<TrainTrips> trips) {
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.trips = new ArrayList<>(trips); // copy to keep it immutable
    }

    public TrainStop getSource() {
        return source;
    }

    public TrainStop getDestination() {
        return destination;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public List<TrainTrips> getTrips() {
        return trips;
    }

    public int getNumberOfTransfers() {
        if (trips.isEmpty()) return 0;
        int transfers = 0;
        String lastRoute = trips.get(0).getRouteNumber();
        for (int i = 1; i < trips.size(); i++) {
            if (!trips.get(i).getRouteNumber().equals(lastRoute)) {
                transfers++;
                lastRoute = trips.get(i).getRouteNumber();
            }
        }
        return transfers;
    }

    public long getTotalDurationMinutes() {
        return Duration.between(departureTime, arrivalTime).toMinutes();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Journey from ").append(source.getName())
        .append(" to ").append(destination.getName()).append("\n");
        sb.append("Departure: ").append(departureTime)
        .append(" | Arrival: ").append(arrivalTime)
        .append(" | Duration: ").append(getTotalDurationMinutes()).append(" minutes\n");
        sb.append("Transfers: ").append(getNumberOfTransfers()).append("\n");
        sb.append("---- Trips ----\n");

        for (int i = 0; i < trips.size(); i++) {
            TrainTrips trip = trips.get(i);
            sb.append(i + 1).append(". [").append(trip.getRouteNumber()).append("] ");
            sb.append(trip.getDepartureTrainStop().getName())
            .append(" -> ").append(trip.getDestinationTrainStop().getName())
            .append(" | Dep: ").append(trip.getDepartureTime())
            .append(" | Dur: ").append(trip.getDuration()).append(" min\n");
        }
        return sb.toString();
    }

}
