package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a journey from source to destination, 
 * including stops, trips, and transfer info.
 */
public class MyCityBusJourney {

    private final MyCityStop source;
    private final MyCityStop destination;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;
    private final List<MyCityTrip> trips; // ordered sequence of trips

    public MyCityBusJourney(MyCityStop source, MyCityStop destination, LocalTime departureTime,LocalTime arrivalTime, List<MyCityTrip> trips) {
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.trips = new ArrayList<>(trips); // copy to keep it immutable
    }

    public MyCityStop getSource() {
        return source;
    }

    public MyCityStop getDestination() {
        return destination;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public List<MyCityTrip> getTrips() {
        return trips;
    }

    public int getNumberOfTransfers() {
        // transfers = trips - 1
        return Math.max(0, trips.size() - 1);
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
            MyCityTrip trip = trips.get(i);
            sb.append(i + 1).append(". ")
              .append(trip.getDepartureStop().getName())
              .append(" -> ").append(trip.getDestinationStop().getName())
              .append(" | Dep: ").append(trip.getDepartureTime())
              .append(" | Dur: ").append(trip.getDuration()).append(" min\n");
        }
        return sb.toString();
    }
}
