package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus;

import com.boolean_brotherhood.public_transportation_journey_planner.BusStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MyCityStop is a BusStop for the "MyCiti Bus Company"
 */
public class MyCityStop extends BusStop {

    private static final String COMPANY = "MyCiti Bus Company";
    private static final String TYPE = "BUS";
    private final List<MyCityTrip> trips = new ArrayList<>();

    /**
     * Constructor for MyCityStop
     *
     * @param name      Name of the stop
     * @param latitude  Latitude
     * @param longitude Longitude
     * @param stopCode  Unique stop code
     * @param address   Address of the stop
     */
    public MyCityStop(String name, double latitude, double longitude, String stopCode, String address) {
        super(name, latitude, longitude, stopCode, address, COMPANY);
    }

    /**
     * Add a MyCityTrip to this stop
     *
     * @param trip MyCityTrip object
     */
    public List<MyCityTrip> getMyCityTrips() {
        return trips;
    }

    public void addMyCityTrip(MyCityTrip trip) {
        if (trip != null) {
            trips.add(trip);
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
        trips.sort(Comparator.comparing(MyCityTrip::getDepartureTime));
    }

    @Override
    public String toString() {
        return String.format(
                "MyCityStop{name='%s', stopCode='%s', company='%s', Lat=%.6f, Lon=%.6f, routes=%s, trips=%d}",
                getName(),
                getStopCode(),
                COMPANY,
                getLatitude(),
                getLongitude(),
                getRouteCodes(),
                getTrips().size()
        );
    }
}
