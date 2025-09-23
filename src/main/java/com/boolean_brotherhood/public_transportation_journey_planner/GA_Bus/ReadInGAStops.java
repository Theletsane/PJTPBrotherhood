package com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class ReadInGAStops{

    public static ArrayList<GAStop> stops = new ArrayList<>();

    public static void loadStopsFromCSV(String filePath) {

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            // skip the header row
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");

                String name = values[1].trim();
                double xcoord = Double.parseDouble(values[2].trim());
                double ycoord = Double.parseDouble(values[3].trim());

                //GAStop stop = new GAStop(name, xcoord, ycoord);
                //stops.add(stop);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //open summary csv
        //use summary to read specific schedule 
    }




    public static void printStops(int thisMany){
        for (int i=0;i<thisMany;i++){
            System.out.println("Stopname: "+stops.get(i).getName());
        }
    }

    public static ArrayList<GAStop> getGAStops(){
        loadStopsFromCSV("Public Transportation Journey Planner/src/main/resources/CapeTownTransitData/MyCiti_Data/no_dup_nameless_stops.csv");
        return stops;

    }

}



