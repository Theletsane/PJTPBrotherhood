package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

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


}