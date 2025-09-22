package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.Objects;

/**
 * A generic Trip between two Stops.
 */
public class Trip {

    private Stop departureStop;
    private Stop destinationStop;
    private int duration;        // in minutes
    private DayType dayType;     // operating day(s)
    private String mode;         // e.g. "Train", "Taxi", "Bus"
    private LocalTime departureTime;

    // ---------------------------
    // Constructors
    // ---------------------------
    public Trip(Stop from, Stop to, int duration, DayType dayType) {
        this.departureStop = from;
        this.destinationStop = to;
        this.duration = duration;
        this.dayType = dayType;
    }

    public Trip(Stop from, Stop to){
        this.departureStop = from;
        this.destinationStop = to;
    }

    public Trip(Stop from, Stop to, DayType dayType) {
        this(from, to, 0, dayType);
    }

    public Trip(Stop from, Stop to, int duration) {
        this(from, to, duration, DayType.WEEKDAY);
    }

    // ---------------------------
    // Getters
    // ---------------------------
    public Stop getDepartureStop() { return departureStop; }
    public Stop getDestinationStop() { return destinationStop; }
    public int getDuration() { return duration; }
    public DayType getDayType() { return dayType; }
    public LocalTime getDepartureTime() { return departureTime; }
    public String getMode() { return mode; }

    // ---------------------------
    // Setters
    // ---------------------------
    public void setDepartureStop(Stop departureStop) { this.departureStop = departureStop; }
    public void setDestinationStop(Stop destinationStop) { this.destinationStop = destinationStop; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setDayType(DayType dayType) { this.dayType = dayType; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public void setMode(String mode) { this.mode = mode; }

    // ---------------------------
    // toString
    // ---------------------------
    @Override
    public String toString() {
        return String.format("Trip: %s -> %s (%d min, %s)",
                departureStop.getName(),
                destinationStop.getName(),
                duration,
                (dayType != null ? dayType : "N/A"));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trip)) return false;
        Trip trip = (Trip) o;
        return duration == trip.duration &&
                Objects.equals(departureStop, trip.departureStop) &&
                Objects.equals(destinationStop, trip.destinationStop) &&
                dayType == trip.dayType &&
                Objects.equals(mode, trip.mode) &&
                Objects.equals(departureTime, trip.departureTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(departureStop, destinationStop, duration, dayType, mode, departureTime);
    }

    // ---------------------------
    // Enum for day types
    // ---------------------------
    public enum DayType {
        WEEKDAY, SATURDAY, SUNDAY, HOLIDAY;

        public static DayType parseDayType(String raw) {
            if (raw == null) return WEEKDAY;
            raw = raw.toUpperCase().trim();

            if (raw.contains("MONDAY") || raw.contains("FRIDAY") || raw.contains("WEEKDAY")) return WEEKDAY;
            if (raw.contains("SATURDAY")) return SATURDAY;
            if (raw.contains("SUNDAY")) return SUNDAY;
            if (raw.contains("HOLIDAY")) return HOLIDAY;

            throw new IllegalArgumentException("Unknown day type: " + raw);
        }

    }




}
