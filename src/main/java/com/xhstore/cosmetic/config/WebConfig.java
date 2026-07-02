package com.xhstore.cosmetic.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Apply authentication check to specific API endpoints
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**", "/api/orders/**", "/api/auth/profile",
                        "/api/auth/password", "/api/custom-orders/**", "/api/reviews/**", "/api/chat/history");
    }
}
