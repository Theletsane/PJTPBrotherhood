package com.boolean_brotherhood.public_transportation_journey_planner.Helpers;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;

// Model classes
@JsonIgnoreProperties(ignoreUnknown = true)
class FeatureCollection {
    public String type;
    public String name;
    public List<Feature> features;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Feature {
    public int id;
    public Geometry geometry;
    public Properties properties;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Geometry {
    public String type;
    public List<Double> coordinates;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Properties {
    public String StationNAm;
    public String StationID;
    public String StationOwn;
    public String SubRegion;
    public String Station;
    public double Latitude;
    public double Longitude;
}

public class GeoJsonReader {
    public static void main(String[] args) {
        try {
            // Load the .geojson file
            File file = new File("C:/Users/thele/OneDrive/Desktop/public-transportation-journey-planner-ptjp/Public Transportation Journey Planner/src/main/resources/CapeTownTransitData/GIS-Maps/Metrorail_Stations_3.geojson"); // <-- replace with your actual path

            ObjectMapper mapper = new ObjectMapper();
            FeatureCollection collection = mapper.readValue(file, FeatureCollection.class);

            int num = 0;
            for (Feature feature : collection.features) {
                ++num;
                System.out.println(
                        "Number: " +num+
                        " Station: " + feature.properties.Station +
                        " (" + feature.properties.StationID + ")" +
                        " | Lat: " + feature.geometry.coordinates.get(1) +
                        " | Lon: " + feature.geometry.coordinates.get(0)
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
