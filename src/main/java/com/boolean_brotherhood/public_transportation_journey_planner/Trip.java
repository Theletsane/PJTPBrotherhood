package com.boolean_brotherhood.public_transportation_journey_planner;

public class Trip {



    private Stop departureStop;
    private Stop destinationStop;
    private int weight;       // travel time, cost, etc
    private DayType dayType;  // when this trip runs
    private String type;

    // ---------------------------
    // Constructors
    // ---------------------------
    public Trip(Stop from, Stop to, int weight, DayType dayType) {
        this.departureStop = from;
        this.destinationStop = to;
        this.weight = weight;
        this.dayType = dayType;
    }

    public Trip(Stop from, Stop to, DayType dayType) {
        this(from, to, 0, dayType);
    }

    public Trip(Stop from, Stop to, int weight) {
        this(from, to, weight, DayType.WEEKDAY); // default to WEEKDAY
    }

    // ---------------------------
    // Getters
    // ---------------------------
    public Stop getDepartureStop() {
        return this.departureStop;
    }

    public Stop getDestinationStop() {
        return this.destinationStop;
    }

    public int getDuration() {
        return this.weight;
    }
    public String getMode(){
        return this.type;
    }

    public DayType getDayType() {
        return dayType;
    }

    // ---------------------------
    // Setters
    // ---------------------------
    public void setDepartureStop(Stop departureStop) {
        this.departureStop = departureStop;
    }

    public void setDestinationStop(Stop destinationStop) {
        this.destinationStop = destinationStop;
    }

    public void setDuration(int weight) {
        this.weight = weight;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    // ---------------------------
    // toString
    // ---------------------------
    @Override
    public String toString() {
        return String.format("Trip: %s -> %s (%d min, %s)",
                this.departureStop.getName(),
                this.destinationStop.getName(),
                weight,
                (dayType != null ? dayType : "N/A"));
    }

    public enum DayType {
        WEEKDAY,
        SATURDAY,
        SUNDAY,
        HOLIDAY;
        public static DayType parseDayType(String raw) {
            if (raw == null) return DayType.WEEKDAY; // default

            raw = raw.toUpperCase().trim();

            if (raw.contains("MONDAY") || raw.contains("FRIDAY") || raw.contains("WEEKDAY")) {
                return DayType.WEEKDAY;
            } else if (raw.contains("SATURDAY")) {
                return DayType.SATURDAY;
            } else if (raw.contains("SUNDAY")) {
                return DayType.SUNDAY;
            } else if (raw.contains("HOLIDAY")) {
                return DayType.HOLIDAY;
            }

            throw new IllegalArgumentException("Unknown day type: " + raw);
        }
    }
}
