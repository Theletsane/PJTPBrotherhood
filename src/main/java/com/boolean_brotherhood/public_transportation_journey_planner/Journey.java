package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Journey is a collection of Trips (edges). The Journey represents a composed route
 * and stores the total duration and transfer information similar to TrainJourney.
 */
public class Journey {
    private final List<Trip> trips;
    private double duration; // total duration (same unit as Trip.getDuration())
    private final Stop source;
    private final Stop destination;
    private final LocalTime departureTime;
    private final LocalTime arrivalTime;

    /**
     * Constructs an empty Journey.
     */
    public Journey() {
        this.trips = new ArrayList<>();
        this.duration = 0.0;
        this.source = null;
        this.destination = null;
        this.departureTime = null;
        this.arrivalTime = null;
    }

    /**
     * Constructs a Journey from an existing list of trips with source, destination, and times.
     *
     * @param source starting stop
     * @param destination ending stop  
     * @param departureTime journey departure time
     * @param arrivalTime journey arrival time
     * @param trips list of Trip objects (may be null or empty)
     */
    public Journey(Stop source, Stop destination, LocalTime departureTime, LocalTime arrivalTime, List<Trip> trips) {
        this.source = source;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.trips = new ArrayList<>();
        this.duration = 0.0;
        
        if (trips != null) {
            for (Trip trip : trips) {
                if (trip != null) {
                    this.trips.add(trip);
                    this.duration += trip.getDuration();
                }
            }
        }
    }

    /**
     * Constructs a Journey from an existing list of trips.
     * A defensive copy of the list is taken.
     *
     * @param trips list of Trip objects (may be null or empty)
     */
    public Journey(List<Trip> trips) {
        this.trips = new ArrayList<>();
        this.duration = 0.0;
        
        if (trips != null && !trips.isEmpty()) {
            this.source = trips.get(0).getDepartureStop();
            this.destination = trips.get(trips.size() - 1).getDestinationStop();
            this.departureTime = trips.get(0).getDepartureTime();
            
            // Calculate arrival time
            LocalTime currentTime = this.departureTime;
            for (Trip trip : trips) {
                if (trip != null) {
                    this.trips.add(trip);
                    this.duration += trip.getDuration();
                    if (currentTime != null) {
                        currentTime = currentTime.plusMinutes(trip.getDuration());
                    }
                }
            }
            this.arrivalTime = currentTime;
        } else {
            this.source = null;
            this.destination = null;
            this.departureTime = null;
            this.arrivalTime = null;
        }
    }

    /**
     * Adds a trip to this journey and updates total duration.
     *
     * @param trip trip to add (ignored if null)
     */
    public void addTrip(Trip trip) {
        if (trip == null) return;
        this.trips.add(trip);
        this.duration += trip.getDuration();
    }

    /**
     * Returns an unmodifiable list of trips composing this journey.
     *
     * @return list of trips (never null)
     */
    public List<Trip> getTrips() {
        return Collections.unmodifiableList(trips);
    }

    /**
     * Returns the source stop of the journey.
     *
     * @return source stop (may be null)
     */
    public Stop getSource() {
        return source;
    }

    /**
     * Returns the destination stop of the journey.
     *
     * @return destination stop (may be null)
     */
    public Stop getDestination() {
        return destination;
    }

    /**
     * Returns the departure time of the journey.
     *
     * @return departure time (may be null)
     */
    public LocalTime getDepartureTime() {
        return departureTime;
    }

    /**
     * Returns the arrival time of the journey.
     *
     * @return arrival time (may be null)
     */
    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    /**
     * Returns the total number of stops in the journey.
     * If there are N trips, there are N+1 stops (unless N == 0 -> 0 stops).
     *
     * @return number of stops visited (0 if journey has no trips)
     */
    public int getTotalStops() {
        if (this.trips.isEmpty()) return 0;
        return this.trips.size() + 1;
    }

    /**
     * Returns the total duration (sum of trip durations).
     *
     * @return duration (same unit as Trip.getDuration())
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Returns the total duration in minutes.
     *
     * @return duration in minutes
     */
    public long getTotalDurationMinutes() {
        if (departureTime != null && arrivalTime != null) {
            return Duration.between(departureTime, arrivalTime).toMinutes();
        }
        return (long) duration;
    }

    /**
     * Calculates the number of transfers in this journey.
     * A transfer occurs when:
     * 1. The mode changes between consecutive trips
     * 2. The route number changes within the same mode (for trips that have route numbers)
     *
     * @return number of transfers
     */
    public int getNumberOfTransfers() {
        if (trips.size() <= 1) return 0;
        
        int transfers = 0;
        for (int i = 1; i < trips.size(); i++) {
            Trip prevTrip = trips.get(i - 1);
            Trip currTrip = trips.get(i);
            
            // Check if mode changed
            String prevMode = getEffectiveMode(prevTrip);
            String currMode = getEffectiveMode(currTrip);
            
            if (!Objects.equals(prevMode, currMode)) {
                transfers++;
            } else {
                // Same mode, check route number if available
                String prevRoute = getRouteNumber(prevTrip);
                String currRoute = getRouteNumber(currTrip);
                
                if (prevRoute != null && currRoute != null && !prevRoute.equals(currRoute)) {
                    transfers++;
                }
            }
        }
        return transfers;
    }

    /**
     * Helper method to get the effective mode of a trip.
     */
    private String getEffectiveMode(Trip trip) {
        if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) {
            return "TRAIN";
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) {
            return "MYCITI";
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) {
            return "GA";
        } else {
            return trip.getMode();
        }
    }

    /**
     * Helper method to get the route number from a trip if available.
     */
    private String getRouteNumber(Trip trip) {
        if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainTrips) trip).getRouteNumber();
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiTrip) trip).getRouteName();
        } else if (trip instanceof com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) {
            return ((com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip) trip).getRouteName();
        }
        return null;
    }

    /**
     * Returns the Trip at the given index (0-based).
     *
     * @param index index of trip
     * @return Trip
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public Trip getTrip(int index) {
        return this.trips.get(index);
    }

    @Override
    public String toString() {
        if (source == null || destination == null) {
            return "Journey{trips=" + trips + ", duration=" + duration + "}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Journey from ").append(source.getName())
          .append(" to ").append(destination.getName()).append("\n");
        sb.append("Departure: ").append(departureTime)
          .append(" | Arrival: ").append(arrivalTime)
          .append(" | Duration: ").append(getTotalDurationMinutes()).append(" minutes\n");
        sb.append("Transfers: ").append(getNumberOfTransfers()).append("\n");
        sb.append("---- Trips ----\n");

        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            sb.append(i + 1).append(". [").append(getEffectiveMode(trip));
            String route = getRouteNumber(trip);
            if (route != null) {
                sb.append(" - ").append(route);
            }
            sb.append("] ");
            sb.append(trip.getDepartureStop().getName())
              .append(" -> ").append(trip.getDestinationStop().getName())
              .append(" | Dep: ").append(trip.getDepartureTime())
              .append(" | Dur: ").append(trip.getDuration()).append(" min\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Journey journey = (Journey) o;
        return Double.compare(journey.duration, duration) == 0 &&
                Objects.equals(trips, journey.trips) &&
                Objects.equals(source, journey.source) &&
                Objects.equals(destination, journey.destination) &&
                Objects.equals(departureTime, journey.departureTime) &&
                Objects.equals(arrivalTime, journey.arrivalTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trips, duration, source, destination, departureTime, arrivalTime);
    }
}