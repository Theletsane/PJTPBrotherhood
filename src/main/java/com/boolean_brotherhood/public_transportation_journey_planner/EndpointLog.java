package com.boolean_brotherhood.public_transportation_journey_planner;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logEndpoint")
public class EndpointLog {

    private static final Map<String, LocalDateTime> endpoint_and_timeCalled = new HashMap<>();
    private static final Map<String, Integer> numberoftimescalled = new HashMap<>();

    public static void LOG(String endpoint_name){
        LocalDateTime timeNow = LocalDateTime.now();
        endpoint_and_timeCalled.put(endpoint_name, timeNow);
        numberoftimescalled.put(endpoint_name, numberoftimescalled.getOrDefault(endpoint_name, 0) + 1);
    }

    private static List<Map.Entry<String, LocalDateTime>> sortByTime() {
        List<Map.Entry<String, LocalDateTime>> sortedEntries = new ArrayList<>(endpoint_and_timeCalled.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue()); // sorts by LocalDateTime
        return sortedEntries;
    }

    @GetMapping("/mostRecentCall")
    public static List<Map<String, Object>> sendLatestTimesCalled() {
        List<Map<String, Object>> response = new ArrayList<>();

        for (Map.Entry<String, LocalDateTime> entry : sortByTime()) {
            Map<String, Object> map = new HashMap<>();
            map.put("endpoint", entry.getKey());
            map.put("lastCalled", entry.getValue());
            map.put("timesCalled", numberoftimescalled.get(entry.getKey()));
            response.add(map);
        }

        return response;
    }
}

