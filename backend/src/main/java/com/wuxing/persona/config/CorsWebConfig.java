package com.wuxing.persona.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsWebConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = {"GET", "POST", "OPTIONS"};
    private static final String[] ALLOWED_HEADERS = {
            "Content-Type",
            "X-Client-Id",
            "X-Session-Id",
            "X-Channel",
            "X-Campaign",
            "X-Admin-Token"
    };
    private static final String[] EXPOSED_HEADERS = {"Content-Disposition", "Location"};

    private final AppProperties properties;

    public CorsWebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = properties.getCors().getAllowedOrigins();
        if (allowedOrigins.isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .exposedHeaders(EXPOSED_HEADERS)
                .maxAge(properties.getCors().getMaxAgeSeconds());
    }
}
