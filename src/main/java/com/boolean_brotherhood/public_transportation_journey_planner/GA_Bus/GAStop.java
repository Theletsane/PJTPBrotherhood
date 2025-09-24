package com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus;



import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.boolean_brotherhood.public_transportation_journey_planner.BusStop;


public class GAStop extends BusStop{

    private static final String COMPANY = "Golden Arrow Bus Company";
    private static final String TYPE = "BUS";
    private final List<GATrip> trips = new ArrayList<>();

    /**
     * Constructor for GA Stop
     *
     * @param name      Name of the stop
     * @param latitude  Latitude
     * @param longitude Longitude
     * @param stopCode  Unique stop code
     * @param address   Address of the stop
     */
    public GAStop(String name, double latitude, double longitude, String stopCode, String address) {
        super(name, latitude, longitude, stopCode, address, COMPANY);
    }

    /**
     * Add a MyCitiTrip to this stop
     *
     * @param trip MyCitiTrip object
     */
    public List<GATrip> getGATrips() {
        return this.trips;
    }

    public void addGATrip(GATrip trip) {
        if (trip != null) {
            this.trips.add(trip);
        }
    }

    public String getCOMPANY() {
        return COMPANY;
    }

    public String getTYPE() {
        return TYPE;
    }

    /** Sort trips by departure time (call after loading all trips) */
    public void sortTripsByDeparture() {
        trips.sort(Comparator.comparing(GATrip::getDepartureTime));
    }

    @Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GAStop other = (GAStop) obj;

    return Double.compare(this.getLatitude(), other.getLatitude()) == 0 &&
           Double.compare(this.getLongitude(), other.getLongitude()) == 0 &&
           this.getName().equals(other.getName()) &&
           this.getStopCode().equals(other.getStopCode());
    }


    
    @Override
    public int hashCode() {
        return Objects.hash(getLatitude(), getLongitude(), getName(), getStopCode());
    }

    @Override
    public String toString() {
        return String.format(
                "Golden Arrow Stop{name='%s', stopCode='%s', company='%s', Lat=%.6f, Lon=%.6f, trips=%d}",
                getName(),
                getStopCode(),
                COMPANY,
                getLatitude(),
                getLongitude(),
                getTrips().size()
        );
}

}
