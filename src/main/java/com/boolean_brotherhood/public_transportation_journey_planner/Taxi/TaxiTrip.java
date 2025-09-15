package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a trip between two Stops, with optional path, times and duration.
 */
public class TaxiTrip extends Trip {
    private TaxiStop departureStop;
    private TaxiStop destinationStop;
    private List<String> road = new ArrayList<>();
    private int duration; // in minutes (documented)
    private String dayOfWeek;
    private String startTime;
    private String endTime;

    public TaxiTrip(TaxiStop departure, TaxiStop destination) {
        super(departure, destination, null);
        this.departureStop =departure;
        this.destinationStop = destination;
    }


    // getters
    public TaxiStop getDepartureStop() { return this.departureStop; }
    public TaxiStop getDestinationStop() { return this.destinationStop; }
    public List<String> getPath() { return Collections.unmodifiableList(road); }
    public int getDuration() { return this.duration; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }

    // setters / mutators
    public void setDuration(int durationMinutes) { this.duration = durationMinutes; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // set/add path
    public void setPath(List<String> road) {
        this.road = new ArrayList<>(road == null ? Collections.emptyList() : road);
    }
    public void addPathSegment(String roadSegment) {
        if (roadSegment != null && !roadSegment.isBlank()) this.road.add(roadSegment);
    }

    @Override
    public String toString() {
        return String.format("Trip[%s -> %s, duration=%.2fmin, path=%s]",
                departureStop.getName(), destinationStop.getName(), duration, road);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip)) return false;
        TaxiTrip trip = (TaxiTrip) o;
        return Objects.equals(departureStop, trip.departureStop) &&
                Objects.equals(destinationStop, trip.destinationStop) &&
                Objects.equals(road, trip.road);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departureStop, destinationStop, road);
    }

    public TaxiStop getDeparture() {
        return departureStop;
    }
    public TaxiStop getDestination() {
        return destinationStop;
    }
}
