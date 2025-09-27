package com.boolean_brotherhood.public_transportation_journey_planner.System;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility builder for consistent metrics responses across controllers.
 */
public final class MetricsResponseBuilder {

    private static final int RECENT_SAMPLE_LIMIT = 25;

    private MetricsResponseBuilder() {
    }

    public static Map<String, Object> build(String scope, Map<String, ?> metrics, String... endpointFragments) {
        return build(scope, metrics, endpointFragments == null ? List.of() : Arrays.asList(endpointFragments));
    }

    public static Map<String, Object> build(String scope, Map<String, ?> metrics, Collection<String> endpointFragments) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", scope);
        response.put("timestamp", Instant.now().toString());
        response.put("metrics", new LinkedHashMap<>(Objects.requireNonNullElse(metrics, Map.of())));
        response.put("performance", buildPerformanceSection(endpointFragments));
        return response;
    }

    private static Map<String, Object> buildPerformanceSection(Collection<String> fragments) {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("overview", PerformanceMetricsRegistry.getOverview());

        Map<String, Object> summaries = PerformanceMetricsRegistry.getEndpointSummaries();
        List<Map<String, Object>> recentSamples = PerformanceMetricsRegistry.getRecentSamples();

        if (fragments == null || fragments.isEmpty()) {
            section.put("endpoints", summaries);
            section.put("recentSamples", trimSamples(recentSamples));
            return section;
        }

        section.put("endpoints", filterSummariesByFragments(summaries, fragments));
        section.put("recentSamples", trimSamples(filterSamplesByFragments(recentSamples, fragments)));
        return section;
    }

    private static Map<String, Object> filterSummariesByFragments(Map<String, Object> summaries,
            Collection<String> fragments) {
        return summaries.entrySet().stream()
                .filter(entry -> matchesFragment(entry.getKey(), fragments))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static List<Map<String, Object>> filterSamplesByFragments(List<Map<String, Object>> samples,
            Collection<String> fragments) {
        return samples.stream()
                .filter(sample -> {
                    Object endpoint = sample.get("endpoint");
                    if (!(endpoint instanceof String key)) {
                        return false;
                    }
                    return matchesFragment(key, fragments);
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static boolean matchesFragment(String text, Collection<String> fragments) {
        if (text == null) {
            return false;
        }
        String lowerText = text.toLowerCase();
        for (String fragment : fragments) {
            if (fragment == null || fragment.isBlank()) {
                continue;
            }
            if (lowerText.contains(fragment.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static List<Map<String, Object>> trimSamples(List<Map<String, Object>> samples) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }
        return samples.stream().limit(RECENT_SAMPLE_LIMIT).collect(Collectors.toCollection(ArrayList::new));
    }
}
