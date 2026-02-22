package com.example.chatstorage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(appProperties.getCors().getAllowedOrigins().toArray(new String[0]))
                .allowedMethods(appProperties.getCors().getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(appProperties.getCors().getAllowedHeaders().toArray(new String[0]))
                .exposedHeaders(appProperties.getCors().getExposedHeaders().toArray(new String[0]))
                .allowCredentials(appProperties.getCors().isAllowCredentials())
                .maxAge(appProperties.getCors().getMaxAgeSeconds());
    }
}
