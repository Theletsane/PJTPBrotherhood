package com.boolean_brotherhood.public_transportation_journey_planner.Taxi;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Taxi-specific Trip.
 */
public class TaxiTrip extends Trip {

    private List<String> road = new ArrayList<>();
    private String dayOfWeek;
    private String startTime;
    private String endTime;

    public TaxiTrip(TaxiStop departure, TaxiStop destination, DayType dayType) {
        super(departure, destination, dayType);
        setMode("Taxi");
    }

    // Extra metadata
    public List<String> getPath() { return Collections.unmodifiableList(road); }
    public void setPath(List<String> road) { this.road = new ArrayList<>(road); }
    public void addPathSegment(String roadSegment) { this.road.add(roadSegment); }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    // Convenience casts
    public TaxiStop getDepartureTaxiStop() { return (TaxiStop) getDepartureStop(); }
    public TaxiStop getDestinationTaxiStop() { return (TaxiStop) getDestinationStop(); }

    @Override
    public String toString() {
        return String.format("TaxiTrip[%s -> %s, duration=%dmin, path=%s]",
                getDepartureStop().getName(),
                getDestinationStop().getName(),
                getDuration(),
                road);
    }
}
