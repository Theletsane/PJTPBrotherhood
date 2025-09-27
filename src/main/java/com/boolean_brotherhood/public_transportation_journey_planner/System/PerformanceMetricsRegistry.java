package com.boolean_brotherhood.public_transportation_journey_planner.System;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects lightweight request timing samples so we can expose recent performance data via REST.
 */
public final class PerformanceMetricsRegistry {

    private static final int MAX_RECENT_SAMPLES = 500;
    private static final String DEFAULT_ENDPOINT = "unknown";

    private static final Deque<PerformanceRecord> RECENT_RECORDS = new ArrayDeque<>();
    private static final Map<String, EndpointStatistics> ENDPOINT_STATS = new ConcurrentHashMap<>();

    private PerformanceMetricsRegistry() {
    }

    /**
     * Record a completed request.
     */
    public static void record(String path, String method, long durationMs, int statusCode) {
        String endpointKey = buildEndpointKey(path, method);
        PerformanceRecord record = new PerformanceRecord(endpointKey, durationMs, statusCode, Instant.now());

        synchronized (RECENT_RECORDS) {
            RECENT_RECORDS.addLast(record);
            while (RECENT_RECORDS.size() > MAX_RECENT_SAMPLES) {
                RECENT_RECORDS.removeFirst();
            }
        }

        ENDPOINT_STATS.compute(endpointKey, (key, stats) -> {
            if (stats == null) {
                stats = new EndpointStatistics(key);
            }
            stats.update(durationMs, statusCode, record.timestamp());
            return stats;
        });
    }

    /**
     * @return most recent samples in chronological order.
     */
    public static List<Map<String, Object>> getRecentSamples() {
        List<Map<String, Object>> snapshot;
        synchronized (RECENT_RECORDS) {
            snapshot = RECENT_RECORDS.stream()
                    .map(PerformanceRecord::toMap)
                    .toList();
        }
        return new ArrayList<>(snapshot);
    }

    /**
     * @return summary map keyed by endpoint signature.
     */
    public static Map<String, Object> getEndpointSummaries() {
        Map<String, Object> summary = new HashMap<>();
        for (Map.Entry<String, EndpointStatistics> entry : ENDPOINT_STATS.entrySet()) {
            summary.put(entry.getKey(), entry.getValue().toMap());
        }
        return summary;
    }

    /**
     * @return overall snapshot containing aggregate counts and latency distribution.
     */
    public static Map<String, Object> getOverview() {
        long totalCount = 0;
        long totalDuration = 0;
        long max = Long.MIN_VALUE;
        long min = Long.MAX_VALUE;
        Instant lastSeen = null;

        for (EndpointStatistics stats : ENDPOINT_STATS.values()) {
            totalCount += stats.getCount();
            totalDuration += stats.getTotalDurationMs();
            max = Math.max(max, stats.getMaxDurationMs());
            min = Math.min(min, stats.getMinDurationMs());
            Instant candidate = stats.getLastObserved();
            if (candidate != null && (lastSeen == null || candidate.isAfter(lastSeen))) {
                lastSeen = candidate;
            }
        }

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalRequests", totalCount);
        overview.put("averageDurationMs", totalCount == 0 ? 0.0 : (double) totalDuration / Math.max(totalCount, 1));
        overview.put("maxDurationMs", max == Long.MIN_VALUE ? 0 : max);
        overview.put("minDurationMs", min == Long.MAX_VALUE ? 0 : min);
        overview.put("lastSampleTimestamp", lastSeen != null ? lastSeen.toString() : null);
        overview.put("trackedEndpoints", ENDPOINT_STATS.size());
        return overview;
    }

    private static String buildEndpointKey(String path, String method) {
        String safePath = (path == null || path.isBlank()) ? DEFAULT_ENDPOINT : path;
        String safeMethod = method == null ? "" : method.toUpperCase();
        return safeMethod + " " + safePath;
    }

    private record PerformanceRecord(String endpointKey, long durationMs, int statusCode, Instant timestamp) {
        Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("endpoint", endpointKey);
            map.put("durationMs", durationMs);
            map.put("status", statusCode);
            map.put("timestamp", timestamp.toString());
            return map;
        }
    }

    private static final class EndpointStatistics {
        private final String endpointKey;
        private long count;
        private long totalDurationMs;
        private long maxDurationMs;
        private long minDurationMs;
        private Instant lastObserved;
        private int lastStatusCode;

        private EndpointStatistics(String endpointKey) {
            this.endpointKey = Objects.requireNonNull(endpointKey);
            this.maxDurationMs = Long.MIN_VALUE;
            this.minDurationMs = Long.MAX_VALUE;
        }

        synchronized void update(long durationMs, int statusCode, Instant observedAt) {
            count++;
            totalDurationMs += durationMs;
            maxDurationMs = Math.max(maxDurationMs, durationMs);
            minDurationMs = Math.min(minDurationMs, durationMs);
            lastObserved = observedAt;
            lastStatusCode = statusCode;
        }

        synchronized Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("endpoint", endpointKey);
            map.put("count", count);
            map.put("averageDurationMs", count == 0 ? 0.0 : (double) totalDurationMs / count);
            map.put("maxDurationMs", maxDurationMs == Long.MIN_VALUE ? 0 : maxDurationMs);
            map.put("minDurationMs", minDurationMs == Long.MAX_VALUE ? 0 : minDurationMs);
            map.put("lastObserved", lastObserved != null ? lastObserved.toString() : null);
            map.put("lastStatus", lastStatusCode);
            return map;
        }

        synchronized long getCount() {
            return count;
        }

        synchronized long getTotalDurationMs() {
            return totalDurationMs;
        }

        synchronized long getMaxDurationMs() {
            return maxDurationMs == Long.MIN_VALUE ? 0 : maxDurationMs;
        }

        synchronized long getMinDurationMs() {
            return minDurationMs == Long.MAX_VALUE ? 0 : minDurationMs;
        }

        synchronized Instant getLastObserved() {
            return lastObserved;
        }
    }
}
