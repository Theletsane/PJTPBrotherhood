package com.boolean_brotherhood.public_transportation_journey_planner;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;

@Configuration
public class GraphConfig {

    @Bean
    public Graph graph() throws Exception {
        Graph g = new Graph();
        g.loadGraphData(); // load stops + trips
        g.buildCombinedGraph(); // precompute connectivity
        return g;
    }

    @Bean
    public TrainGraph trainGraph(Graph graph) {
        return graph.getTrainGraph(); // reuse same graph instance
    }

    @Bean
    public MyCitiBusGraph myCitiBusGraph(Graph graph) {
        MyCitiBusGraph myCitiGraph = graph.getMyCitiBusGraph(); // Fixed: use 'myCitiGraph' instead of 'g'
        if (myCitiGraph == null) {
            System.err.println(" MyCitiBusGraph is NULL when GraphConfig runs!");
        } else {
            System.out.println(" MyCitiBusGraph loaded with " + myCitiGraph.getMyCitiStops().size() + " stops");
        }
        return myCitiGraph;
    }

    @Bean
    public TaxiGraph taxiGraph(Graph graph) {
        return graph.getTaxiGraph();
    }

    @Bean
    public GABusGraph gaBusGraph(Graph graph) {
        return graph.getGABusGraph();
    }

    @Bean
    public BusGraph busGraph(Graph graph) {
        return graph.getBusGraph();
    }
}