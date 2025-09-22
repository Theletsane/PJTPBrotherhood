package com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus;

import com.boolean_brotherhood.public_transportation_journey_planner.BusStop;
import com.boolean_brotherhood.public_transportation_journey_planner.Trip;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MyCitiStop is a BusStop for the "MyCiti Bus Company"
 */
public class MyCitiStop extends BusStop {

    private static final String COMPANY = "MyCiti Bus Company";
    private static final String TYPE = "BUS";
    private final List<MyCitiTrip> trips = new ArrayList<>();

    /**
     * Constructor for MyCitiStop
     *
     * @param name      Name of the stop
     * @param latitude  Latitude
     * @param longitude Longitude
     * @param stopCode  Unique stop code
     * @param address   Address of the stop
     */
    public MyCitiStop(String name, double latitude, double longitude, String stopCode, String address) {
        super(name, latitude, longitude, stopCode, address, COMPANY);
    }

    /**
     * Add a MyCitiTrip to this stop
     *
     * @param trip MyCitiTrip object
     */
    public List<MyCitiTrip> getMyCitiTrips() {
        return trips;
    }

    public void addMyCitiTrip(MyCitiTrip trip) {
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
        trips.sort(Comparator.comparing(MyCitiTrip::getDepartureTime));
    }

    @Override
    public String toString() {
        return String.format(
                "MyCitiStop{name='%s', stopCode='%s', company='%s', Lat=%.6f, Lon=%.6f, routes=%s, trips=%d}",
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
