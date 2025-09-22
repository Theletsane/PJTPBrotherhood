package com.boolean_brotherhood.public_transportation_journey_planner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import com.boolean_brotherhood.public_transportation_journey_planner.MyCitiBus.MyCitiBusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Taxi.TaxiGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.Train.TrainGraph;

@Configuration
public class GraphConfig {

    @Bean
    public Graph graph() throws Exception {
        Graph g = new Graph();
        g.loadGraphData();       // load stops + trips
        g.buildCombinedGraph();  // precompute connectivity
        return g;
    }

    @Bean
    public TrainGraph trainGraph(Graph graph) {
        return graph.getTrainGraph(); // reuse same graph instance
    }

    @Bean
    public MyCitiBusGraph MyCitiBusGraph(Graph graph) {
        return graph.getMyCitiBusGraph();
    }

    @Bean
    public TaxiGraph taxiGraph(Graph graph){
        return graph.getTaxiGraph();
    }
}
