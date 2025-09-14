package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;


import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainStop;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoService performs place -> coordinates lookups (Nominatim) and Overpass queries
 * to find nearby transport stops. Designed to be a single responsibility service:
 * - getCoordinates(placeName) -> [lat, lon] or null
 * - getNearbyStops(lat, lon, radiusMeters, retries, type) -> Map<Stop, distanceKm>
 *
 * NOTE: These methods perform network I/O and should be treated as integration calls.
 * For unit testing you can mock this class or call with invalid inputs (non-network tests).
 */
public class GeoService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; PTJP/1.0; +https://example.org)";
    // Nominatim usage policy asks for a valid user-agent and limited query rate.
    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * Query Nominatim (OpenStreetMap) to get coordinates for a place name.
     * Returns a double[2] {lat, lon} or null if not found or on error.
     */
    public static double[] getCoordinates(String placeName) {
        if (placeName == null || placeName.isBlank()) return null;
        BufferedReader in = null;
        try {
            // Add context for Cape Town to improve results if not already there
            String query = placeName + ", Cape Town, South Africa";
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                    URLEncoder.encode(query, "UTF-8");

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
            conn.setReadTimeout(DEFAULT_TIMEOUT_MS);

            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);

            JSONArray arr = new JSONArray(response.toString());
            if (arr.length() == 0) {
                return null;
            }
            JSONObject obj = arr.getJSONObject(0);
            double lat = obj.optDouble("lat", Double.NaN);
            double lon = obj.optDouble("lon", Double.NaN);
            if (Double.isFinite(lat) && Double.isFinite(lon)) {
                return new double[]{lat, lon};
            } else {
                return null;
            }
        } catch (Exception e) {
            // log and return null on error (caller should handle null)
            System.err.println("GeoService.getCoordinates error: " + e.getMessage());
            return null;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
        }
    }



    // private static final String USER_AGENT = "Mozilla/5.0 (compatible; PTJP/1.0; +https://example.org)";
    //private static final int DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * Query Overpass (via kumi.systems endpoint) for nearby stops of given type.
     * Prints all found stops with their coordinates.
     */
    public static List<TrainStop> getNearbyStops(double lat, double lon, int radiusMeters, int retries, String type) {
        String query;
        switch (type) {
            case "Bus":
                query = String.format("[out:json];node[\"highway\"=\"bus_stop\"](around:%d,%f,%f);out;",
                        radiusMeters, lat, lon);
                break;
            case "Taxi":
                query = String.format("[out:json];node[\"amenity\"=\"taxi\"](around:%d,%f,%f);out;",
                        radiusMeters, lat, lon);
                break;
            case "Train":
                query = String.format("[out:json];node[\"railway\"=\"station\"](around:%d,%f,%f);out;",
                        radiusMeters, lat, lon);
                break;
            default:
                System.err.println("GeoService.getNearbyStops: unknown transport type: " + type);
                return null;
        }
        List<TrainStop> g = new ArrayList<>();
        for (int attempt = 1; attempt <= Math.max(1, retries); attempt++) {
            BufferedReader br = null;
            try {
                String urlStr = "https://overpass.kumi.systems/api/interpreter?data=" +
                        URLEncoder.encode(query, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setConnectTimeout(DEFAULT_TIMEOUT_MS);
                conn.setReadTimeout(DEFAULT_TIMEOUT_MS);

                br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder json = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) json.append(line);

                JSONObject obj = new JSONObject(json.toString());
                JSONArray elements = obj.optJSONArray("elements");

                if (elements != null) {
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject el = elements.getJSONObject(i);
                        double stopLat = el.optDouble("lat");
                        double stopLon = el.optDouble("lon");
                        String stopName = el.optJSONObject("tags") != null ?
                                el.optJSONObject("tags").optString("name", "Unnamed Stop")
                                : "Unnamed Stop";

                        g.add(new TrainStop("Train",stopLat,stopLon,stopName,i+"G"));
                        //System.out.printf("Stop: %s (Lat: %.6f, Lon: %.6f)%n", stopName, stopLat, stopLon);
                    }
                } else {
                    System.out.println("No stops found.");
                }

            } catch (Exception e) {
                System.err.println("GeoService.getNearbyStops attempt " + attempt +
                        " failed: " + e.getMessage());
                if (attempt < retries) {
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException ignored) {}
                }
            } finally {
                try {
                    if (br != null) br.close();
                } catch (Exception ignored) {}
            }
        }
        return g;
    }
}