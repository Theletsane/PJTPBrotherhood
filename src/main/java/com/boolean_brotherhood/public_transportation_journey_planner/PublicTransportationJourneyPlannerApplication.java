package com.boolean_brotherhood.public_transportation_journey_planner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PublicTransportationJourneyPlannerApplication implements CommandLineRunner {

    
    public static void main(String[] args) {
        SpringApplication.run(PublicTransportationJourneyPlannerApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        // This runs after Spring Boot starts
        System.out.println(" Spring Boot started successfully!");
        System.out.println(" Graph has been loaded successfully!");
    }

    
}
