package com.boolean_brotherhood.public_transportation_journey_planner.System;

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor that measures request latency and stores it in the shared registry.
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = RequestTimingInterceptor.class.getName() + ".start";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        request.setAttribute(START_TIME_ATTR, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            @Nullable Exception ex) throws Exception {
        Object startAttr = request.getAttribute(START_TIME_ATTR);
        if (startAttr instanceof Long start) {
            long durationNs = System.nanoTime() - start;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNs);
            PerformanceMetricsRegistry.record(request.getRequestURI(), request.getMethod(), durationMs,
                    response.getStatus());
        }
    }
}
