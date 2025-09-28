package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boolean_brotherhood.public_transportation_journey_planner.Trip;
import com.boolean_brotherhood.public_transportation_journey_planner.TransportGraphTester;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusGraph;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GABusJourney;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GAStop;
import com.boolean_brotherhood.public_transportation_journey_planner.GA_Bus.GATrip;
import com.boolean_brotherhood.public_transportation_journey_planner.System.MetricsResponseBuilder;
import com.boolean_brotherhood.public_transportation_journey_planner.System.SystemLog;


@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TransportGraphTester tester;

    // Constructor injection (best practice)
    public TestController(TransportGraphTester tester) {
        this.tester = tester;
    }

    @GetMapping("/test-interactive")
    public String testInteractive() {
        tester.runInteractiveTest();
        return "Check console output";
    }

    @GetMapping("/test-quick")
    public String testQuick() {
        tester.runAllQuickTests();
        return "Check console output";
    }

    @GetMapping("/test-myciti")
    public String testMyCiti() {
        tester.quickTestMyCiti();
        return "Check console output";
    }
}