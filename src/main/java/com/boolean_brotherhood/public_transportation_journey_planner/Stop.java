package com.boolean_brotherhood.public_transportation_journey_planner;

import java.util.List;

/**
 * A stop object represent a node of Stop for different modes of transport(taxi,train,bus).
 *
 *
 *
 * @author Itumeleng Theletsane
 * @version 8/13/2025
 */

public class Stop {
    private String name;
    private double latitude;
    private double longitude;
    private String type;
    public String stopcode;
    private List<Trip> TRIPS;
    private String address;

    /*
     *Creates a constructor using the type of transportation, name of the company, latitude, longitude, stop name, stop code
     */
    public Stop(String type, double lat, double lon,String stopName , String stopCode){
        this.stopcode = stopCode;
        this.longitude = lon;
        this.latitude = lat;
        this.name = stopName;
        this.type = type;
    }

    public Stop(String type, double lat, double lon,String stopName , String stopCode,String address){
        this.stopcode = stopCode;
        this.longitude = lon;
        this.latitude = lat;
        this.name = stopName;
        this.type = type;
        this.address = address;
    }

    /*
     * Creates the Stop object representing nodes in a graph
     * @Args :  code, name ,type
     */
    public Stop(String type, String name, String code) {
        this.name = name;
        this.stopcode = code;
        switch (type) {
            case "Train":
            case "Bus":
            case "Taxi":
                this.type = type;
                break;
            default:
                this.type = null; // invalid type
        }
    }

    /*
     *Returns the name of the stop
     */
    public String getName() {
        return this.name;
    }

    /*
     *Returns the latitude of the stop
     */
    public double getLatitude() {
        return latitude;
    }

    /*
     *Returns the Longitude of the stop
     */
    public double getLongitude() {
        return longitude;
    }

    /*
     *Returns the type of the stop(Train, Bus, Taxi)
     */
    public String getType() {
        return type;
    }

    /*
     *Sets the stopCode for stop
     */
    public String getStopcode() {
        return this.stopcode ;
    }


    public List<Trip> getTripsFromStop(){
        return TRIPS;
    }

    public String getStopAddress(){
        return this.address;
    }

    /*
     *Sets the stopCode for stop
     */
    public void setStopcode( String stopcode) {
        this.stopcode = stopcode;
    }


    public void addTrip(Trip trip){
        TRIPS.add(trip);
    }

    /*
     *Setting the co-ordinates of a stop
     */
    public void setStopCoOrdinates(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;
    }

    /*
     *Sets the latitude of the stop
     */
    public void setLatitude(double lat) {
        this.latitude  = lat;
    }

    /*
     *Sets the Longitude of the stop
     */
    public void setLongitude(double lon) {
        this.longitude  = lon;
    }

    /*
     *This method calculates the distance between two Stop.
     * It accepts an Object of Stop, and return the distance in Km
     */
    public double getDistanceBetween(Stop next){

        final int R = 6371;
        double dLat = Math.toRadians(this.latitude - next.getLatitude());
        double dLon = Math.toRadians(this.longitude - next.getLongitude());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(next.getLatitude())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /*
     *This method calculates the distance between two Stop.
     * It accepts two number latitude and longitude respectively, of a specific co-ordinate. Return the distance in Km
     */
    public double getDistanceBetween(double nextLat, double nextLon){

        final int R = 6371; // km
        double dLat = Math.toRadians(this.latitude - nextLat);
        double dLon = Math.toRadians(this.longitude - nextLon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(nextLat)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;

    }

    /*
     *This is a toString method and returns a stringified version of the object.
     * e.g "Umlazi stop: Train - 252.5, 582.55"
     */
    @Override
    public String toString() {
        return String.format("%s %s stop code: %s \nvehicle type: (%s) - Lat: %.6f, Lon: %.6f", name, stopcode, type, latitude, longitude);
    }
}
