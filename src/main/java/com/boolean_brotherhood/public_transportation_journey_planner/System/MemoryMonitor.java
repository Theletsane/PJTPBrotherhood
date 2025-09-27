package com.boolean_brotherhood.public_transportation_journey_planner.System;


import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

public class MemoryMonitor {
    
    public static Map<String, Object> getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> stats = new HashMap<>();
        
        // Runtime memory
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        stats.put("maxMemoryMB", maxMemory / (1024 * 1024));
        stats.put("totalMemoryMB", totalMemory / (1024 * 1024));
        stats.put("usedMemoryMB", usedMemory / (1024 * 1024));
        stats.put("freeMemoryMB", freeMemory / (1024 * 1024));
        stats.put("usagePercent", Math.round((double) usedMemory / totalMemory * 100));
        
        // Detailed heap memory
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        stats.put("heapUsedMB", heapMemory.getUsed() / (1024 * 1024));
        stats.put("heapMaxMB", heapMemory.getMax() / (1024 * 1024));
        stats.put("heapCommittedMB", heapMemory.getCommitted() / (1024 * 1024));
        
        // Non-heap memory
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        stats.put("nonHeapUsedMB", nonHeapMemory.getUsed() / (1024 * 1024));
        stats.put("nonHeapMaxMB", nonHeapMemory.getMax() / (1024 * 1024));
        
        return stats;
    }
    
    public static void forceGC() {
        System.gc();
        System.runFinalization();
        System.gc();
    }
}