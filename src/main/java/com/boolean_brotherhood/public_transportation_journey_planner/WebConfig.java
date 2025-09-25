package com.boolean_brotherhood.public_transportation_journey_planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers application-wide MVC configuration such as request timing.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestTimingInterceptor requestTimingInterceptor;

    @Autowired
    public WebConfig(RequestTimingInterceptor requestTimingInterceptor) {
        this.requestTimingInterceptor = requestTimingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTimingInterceptor).addPathPatterns("/**");
    }
}
