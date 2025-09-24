
/**
 * RouteCoordinateExtractor.java
 * 
 * This class handles extraction of railway route coordinates from GeoJSON data.
 * It provides methods to get coordinate paths between train stops for mapping purposes.
 * 
 * Author: Boolean Brotherhood
 * Date: 2025
 */
package com.boolean_brotherhood.public_transportation_journey_planner.Train;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.DataFilesRegistry;
import com.boolean_brotherhood.public_transportation_journey_planner.Helpers.MyFileLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RouteCoordinateExtractor {

    private JsonNode railwayGeoJson;
    private final String railwayGeoJsonFile;
    private final ObjectMapper objectMapper;

    /**
     * Constructor initializes the GeoJSON file path
     */
    public RouteCoordinateExtractor() {
        String usedBy = this.getClass().getSimpleName();
        this.railwayGeoJsonFile = DataFilesRegistry.getFile("RAILWAY_GEOJSON", usedBy);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Constructor with custom GeoJSON file path
     */
    public RouteCoordinateExtractor(String geoJsonFilePath) {
        this.railwayGeoJsonFile = geoJsonFilePath;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Represents a coordinate point with latitude and longitude
     */
    public static class Coordinate {
        private final double longitude;
        private final double latitude;
        
        public Coordinate(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
        
        public double getLongitude() { return longitude; }
        public double getLatitude() { return latitude; }
        
        @Override
        public String toString() {
            return String.format("[%.6f, %.6f]", longitude, latitude);
        }
        
        // Calculate distance to another coordinate using Haversine formula
        public double distanceTo(Coordinate other) {
            return distanceTo(other.latitude, other.longitude);
        }
        
        public double distanceTo(double lat, double lon) {
            final int R = 6371; // Earth's radius in km
            double latDistance = Math.toRadians(lat - this.latitude);
            double lonDistance = Math.toRadians(lon - this.longitude);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(lat))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Coordinate that = (Coordinate) obj;
            return Double.compare(that.longitude, longitude) == 0 &&
                   Double.compare(that.latitude, latitude) == 0;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(longitude, latitude);
        }
    }

    /**
     * Represents a route segment with coordinates and metadata
     */
    public static class RouteSegment {
        private final List<Coordinate> coordinates;
        private final String routeName;
        private final int lineId;
        private final double totalDistance;
        
        public RouteSegment(List<Coordinate> coordinates, String routeName, int lineId) {
            this.coordinates = new ArrayList<>(coordinates);
            this.routeName = routeName;
            this.lineId = lineId;
            this.totalDistance = calculateTotalDistance();
        }
        
        public List<Coordinate> getCoordinates() { return new ArrayList<>(coordinates); }
        public String getRouteName() { return routeName; }
        public int getLineId() { return lineId; }
        public double getTotalDistance() { return totalDistance; }
        
        private double calculateTotalDistance() {
            double total = 0.0;
            for (int i = 0; i < coordinates.size() - 1; i++) {
                total += coordinates.get(i).distanceTo(coordinates.get(i + 1));
            }
            return total;
        }

        public boolean isEmpty() {
            return coordinates.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("RouteSegment{routeName='%s', lineId=%d, coordinates=%d, distance=%.2fkm}", 
                               routeName, lineId, coordinates.size(), totalDistance);
        }
    }

    /**
     * Loads the railway GeoJSON data into memory for route coordinate extraction
     */
    public void loadRailwayGeoJson() throws IOException {
        try (BufferedReader br = MyFileLoader.getBufferedReaderFromResource(railwayGeoJsonFile)) {
            railwayGeoJson = objectMapper.readTree(br);
        }
    }

    /**
     * Extracts route coordinates between two stops from a train trip
     * 
     * @param trip The train trip to get coordinates for
     * @return RouteSegment containing the coordinates between departure and destination stops
     * @throws IOException if GeoJSON data cannot be read
     */
    public RouteSegment getRouteCoordinates(TrainTrips trip) throws IOException {
        if (trip == null) {
            throw new IllegalArgumentException("Trip cannot be null");
        }

        TrainStop departureStop = trip.getDepartureTrainStop();
        TrainStop destinationStop = trip.getDestinationTrainStop();
        String routeNumber = trip.getRouteNumber();
        
        return getRouteCoordinates(departureStop, destinationStop, routeNumber);
    }

    /**
     * Extracts route coordinates between two stops for a specific route
     * 
     * @param departureStop Starting train stop
     * @param destinationStop Ending train stop  
     * @param routeNumber Route identifier to match against GeoJSON
     * @return RouteSegment containing the coordinates between the stops
     * @throws IOException if GeoJSON data cannot be read
     */
    public RouteSegment getRouteCoordinates(TrainStop departureStop, TrainStop destinationStop, String routeNumber) 
            throws IOException {
        
        if (departureStop == null || destinationStop == null) {
            throw new IllegalArgumentException("Departure and destination stops cannot be null");
        }

        if (railwayGeoJson == null) {
            loadRailwayGeoJson();
        }
        
        // Find the matching railway line in GeoJSON
        JsonNode features = railwayGeoJson.get("features");
        
        if (features == null || !features.isArray()) {
            throw new IOException("Invalid GeoJSON format: features array not found");
        }
        
        for (JsonNode feature : features) {
            JsonNode properties = feature.get("properties");
            if (properties == null) continue;
            
            JsonNode lineNameNode = properties.get("LINENAME");
            if (lineNameNode == null) continue;
            
            String lineName = lineNameNode.asText();
            
            // Match route by name
            if (isRouteMatch(lineName, routeNumber)) {
                JsonNode geometry = feature.get("geometry");
                if (geometry == null) continue;
                
                JsonNode coordinatesNode = geometry.get("coordinates");
                if (coordinatesNode == null) continue;
                
                List<Coordinate> allCoordinates = extractCoordinates(coordinatesNode);
                List<Coordinate> segmentCoordinates = extractSegmentBetweenStops(
                    allCoordinates, departureStop, destinationStop);
                
                JsonNode lineIdNode = properties.get("LINEID");
                int lineId = lineIdNode != null ? lineIdNode.asInt() : -1;
                
                return new RouteSegment(segmentCoordinates, lineName, lineId);
            }
        }
        
        // If no exact match found, return coordinates for the closest matching route
        return getClosestRouteCoordinates(departureStop, destinationStop);
    }

    /**
     * Extracts all coordinates from a GeoJSON coordinates array
     */
    private List<Coordinate> extractCoordinates(JsonNode coordinatesNode) {
        List<Coordinate> coordinates = new ArrayList<>();
        
        if (coordinatesNode != null && coordinatesNode.isArray()) {
            for (JsonNode coordPair : coordinatesNode) {
                if (coordPair.isArray() && coordPair.size() >= 2) {
                    double lon = coordPair.get(0).asDouble();
                    double lat = coordPair.get(1).asDouble();
                    coordinates.add(new Coordinate(lon, lat));
                }
            }
        }
        
        return coordinates;
    }

    /**
     * Extracts the segment of coordinates between two train stops
     */
    private List<Coordinate> extractSegmentBetweenStops(List<Coordinate> allCoordinates, 
                                                       TrainStop departureStop, 
                                                       TrainStop destinationStop) {
        
        if (allCoordinates.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the closest coordinate points to each stop
        int startIndex = findClosestCoordinateIndex(allCoordinates, departureStop);
        int endIndex = findClosestCoordinateIndex(allCoordinates, destinationStop);
        
        // Ensure proper ordering
        if (startIndex > endIndex) {
            int temp = startIndex;
            startIndex = endIndex;
            endIndex = temp;
        }
        
        // Extract the segment
        List<Coordinate> segment = new ArrayList<>();
        for (int i = startIndex; i <= endIndex && i < allCoordinates.size(); i++) {
            segment.add(allCoordinates.get(i));
        }
        
        return segment;
    }

    /**
     * Finds the index of the coordinate closest to a train stop
     */
    private int findClosestCoordinateIndex(List<Coordinate> coordinates, TrainStop stop) {
        if (coordinates.isEmpty()) {
            return 0;
        }

        double minDistance = Double.MAX_VALUE;
        int closestIndex = 0;
        
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coord = coordinates.get(i);
            double distance = coord.distanceTo(stop.getLatitude(), stop.getLongitude());
            
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }
        
        return closestIndex;
    }

    /**
     * Checks if a route line name matches the given route number
     */
    private boolean isRouteMatch(String lineName, String routeNumber) {
        if (lineName == null || routeNumber == null) {
            return false;
        }
        
        // Direct match
        if (lineName.contains(routeNumber)) {
            return true;
        }
        
        // Try to match common patterns
        String normalizedLineName = lineName.toLowerCase().replaceAll("[\\s-_]", "");
        String normalizedRouteNumber = routeNumber.toLowerCase().replaceAll("[\\s-_]", "");
        
        return normalizedLineName.contains(normalizedRouteNumber) || 
               normalizedRouteNumber.contains(normalizedLineName);
    }

    /**
     * Gets coordinates for the closest matching route when no exact match is found
     */
    private RouteSegment getClosestRouteCoordinates(TrainStop departureStop, TrainStop destinationStop) 
            throws IOException {
        
        JsonNode features = railwayGeoJson.get("features");
        
        double minDistance = Double.MAX_VALUE;
        RouteSegment closestSegment = null;
        
        for (JsonNode feature : features) {
            JsonNode properties = feature.get("properties");
            if (properties == null) continue;
            
            JsonNode lineNameNode = properties.get("LINENAME");
            JsonNode lineIdNode = properties.get("LINEID");
            
            String lineName = lineNameNode != null ? lineNameNode.asText() : "Unknown";
            int lineId = lineIdNode != null ? lineIdNode.asInt() : -1;
            
            JsonNode geometry = feature.get("geometry");
            if (geometry == null) continue;
            
            JsonNode coordinatesNode = geometry.get("coordinates");
            if (coordinatesNode == null) continue;
            
            List<Coordinate> allCoordinates = extractCoordinates(coordinatesNode);
            
            if (!allCoordinates.isEmpty()) {
                // Calculate distance from stops to this route
                double routeDistance = calculateDistanceToRoute(allCoordinates, departureStop, destinationStop);
                
                if (routeDistance < minDistance) {
                    minDistance = routeDistance;
                    List<Coordinate> segmentCoordinates = extractSegmentBetweenStops(
                        allCoordinates, departureStop, destinationStop);
                    closestSegment = new RouteSegment(segmentCoordinates, lineName, lineId);
                }
            }
        }
        
        return closestSegment != null ? closestSegment : 
               new RouteSegment(new ArrayList<>(), "Unknown", -1);
    }

    /**
     * Calculates the average distance from two stops to a route
     */
    private double calculateDistanceToRoute(List<Coordinate> routeCoordinates, 
                                          TrainStop stop1, TrainStop stop2) {
        double minDist1 = Double.MAX_VALUE;
        double minDist2 = Double.MAX_VALUE;
        
        for (Coordinate coord : routeCoordinates) {
            double dist1 = coord.distanceTo(stop1.getLatitude(), stop1.getLongitude());
            double dist2 = coord.distanceTo(stop2.getLatitude(), stop2.getLongitude());
            
            minDist1 = Math.min(minDist1, dist1);
            minDist2 = Math.min(minDist2, dist2);
        }
        
        return (minDist1 + minDist2) / 2.0;
    }

    /**
     * Utility method to get route coordinates for an entire journey
     * 
     * @param journey The complete train journey
     * @return List of RouteSegment objects for each trip in the journey
     * @throws IOException if GeoJSON data cannot be read
     */
    public List<RouteSegment> getJourneyRouteCoordinates(TrainJourney journey) throws IOException {
        if (journey == null) {
            throw new IllegalArgumentException("Journey cannot be null");
        }

        List<RouteSegment> segments = new ArrayList<>();
        
        for (TrainTrips trip : journey.getTrips()) {
            RouteSegment segment = getRouteCoordinates(trip);
            segments.add(segment);
        }
        
        return segments;
    }

    /**
     * Converts route coordinates to a format suitable for mapping libraries (like Leaflet)
     * Returns coordinates as [lat, lon] pairs (standard for most mapping libraries)
     * 
     * @param segment The route segment to convert
     * @return JSON string representation of coordinates
     */
    public String getRouteCoordinatesAsJson(RouteSegment segment) {
        if (segment == null || segment.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder();
        json.append("[");
        
        List<Coordinate> coordinates = segment.getCoordinates();
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate coord = coordinates.get(i);
            json.append(String.format("[%.6f, %.6f]", coord.getLatitude(), coord.getLongitude()));
            
            if (i < coordinates.size() - 1) {
                json.append(", ");
            }
        }
        
        json.append("]");
        return json.toString();
    }

    /**
     * Gets all available route names from the GeoJSON data
     * 
     * @return List of route names
     * @throws IOException if GeoJSON data cannot be read
     */
    public List<String> getAvailableRoutes() throws IOException {
        if (railwayGeoJson == null) {
            loadRailwayGeoJson();
        }

        List<String> routes = new ArrayList<>();
        JsonNode features = railwayGeoJson.get("features");
        
        if (features != null && features.isArray()) {
            for (JsonNode feature : features) {
                JsonNode properties = feature.get("properties");
                if (properties != null) {
                    JsonNode lineNameNode = properties.get("LINENAME");
                    if (lineNameNode != null) {
                        String lineName = lineNameNode.asText();
                        if (!routes.contains(lineName)) {
                            routes.add(lineName);
                        }
                    }
                }
            }
        }
        
        return routes;
    }

    /**
     * Gets the complete route coordinates for a specific route name
     * 
     * @param routeName The name of the route
     * @return RouteSegment with complete route coordinates
     * @throws IOException if GeoJSON data cannot be read
     */
    public RouteSegment getCompleteRouteCoordinates(String routeName) throws IOException {
        if (routeName == null) {
            throw new IllegalArgumentException("Route name cannot be null");
        }

        if (railwayGeoJson == null) {
            loadRailwayGeoJson();
        }

        JsonNode features = railwayGeoJson.get("features");
        
        if (features != null && features.isArray()) {
            for (JsonNode feature : features) {
                JsonNode properties = feature.get("properties");
                if (properties == null) continue;
                
                JsonNode lineNameNode = properties.get("LINENAME");
                if (lineNameNode == null) continue;
                
                String lineName = lineNameNode.asText();
                
                if (lineName.equals(routeName)) {
                    JsonNode geometry = feature.get("geometry");
                    if (geometry == null) continue;
                    
                    JsonNode coordinatesNode = geometry.get("coordinates");
                    if (coordinatesNode == null) continue;
                    
                    List<Coordinate> coordinates = extractCoordinates(coordinatesNode);
                    JsonNode lineIdNode = properties.get("LINEID");
                    int lineId = lineIdNode != null ? lineIdNode.asInt() : -1;
                    
                    return new RouteSegment(coordinates, lineName, lineId);
                }
            }
        }
        
        return new RouteSegment(new ArrayList<>(), routeName, -1);
    }
    
}