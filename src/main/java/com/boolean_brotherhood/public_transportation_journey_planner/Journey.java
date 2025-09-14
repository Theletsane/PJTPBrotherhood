package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A Journey is a collection of Trips (edges). The Journey represents a composed route
 * and stores the total duration (sum of trip durations).
 *
 * Notes:
 *  - duration is the sum of trip.getDuration(); document the unit used (minutes recommended).
 *  - many getters return null if the journey has no trips (e.g., getStartTime()).
 */
public class Journey {
    private final List<Trip> trips;
    private double duration; // total duration (same unit as Trip.getDuration())

    /**
     * Constructs an empty Journey.
     */
    public Journey() {
        this.trips = new ArrayList<>();
        this.duration = 0.0;
    }

    /**
     * Constructs a Journey from an existing list of trips.
     * A defensive copy of the list is taken.
     *
     * @param trips list of Trip objects (may be null or empty)
     */
    public Journey(List<Trip> trips) {
        this();
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
        return "Journey{" +
                "trips=" + trips +
                ", duration=" + duration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Journey journey = (Journey) o;
        return Double.compare(journey.duration, duration) == 0 &&
                Objects.equals(trips, journey.trips);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trips, duration);
    }
}
