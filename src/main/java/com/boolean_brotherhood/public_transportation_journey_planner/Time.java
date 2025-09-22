package com.boolean_brotherhood.public_transportation_journey_planner;

public class Time {
    private int hours;   // 0 - 23
    private int minutes; // 0 - 59

    // Constructor from string "HH:MM"
    public Time(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            throw new IllegalArgumentException("Time must be in HH:MM format");
        }

        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);

        if (h < 0 || h > 23 || m < 0 || m > 59) {
            throw new IllegalArgumentException("Invalid time values: " + time);
        }

        this.hours = h;
        this.minutes = m;
    }

    // Getters
    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    // Convert to total minutes since 00:00
    private int toMinutes() {
        return hours * 60 + minutes;
    }

    // Difference in minutes (absolute value)
    public int differenceInMinutes(Time other) {
        return Math.abs(this.toMinutes() - other.toMinutes());
    }

    // Compare equality
    public boolean equals(Time other) {
        return this.hours == other.hours && this.minutes == other.minutes;
    }

    // Check if this time is before another
    public boolean isBefore(Time other) {
        return this.toMinutes() < other.toMinutes();
    }

    // Check if this time is after another
    public boolean isAfter(Time other) {
        return this.toMinutes() > other.toMinutes();
    }

    // toString in HH:MM format
    @Override
    public String toString() {
        return String.format("%02d:%02d", hours, minutes);
    }
}
