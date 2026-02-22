package com.example.chatstorage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final RateLimit rateLimit = new RateLimit();
    private final Cors cors = new Cors();

    public Security getSecurity() {
        return security;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Cors getCors() {
        return cors;
    }

    public static class Security {
        private String adminApiKey;
        private String apiKeyPepper;

        public String getAdminApiKey() {
            return adminApiKey;
        }

        public void setAdminApiKey(String adminApiKey) {
            this.adminApiKey = adminApiKey;
        }

        public String getApiKeyPepper() {
            return apiKeyPepper;
        }

        public void setApiKeyPepper(String apiKeyPepper) {
            this.apiKeyPepper = apiKeyPepper;
        }
    }

    public static class RateLimit {
        private int requestsPerMinute = 120;
        private int windowSeconds = 60;
        private boolean failOpenWhenRedisDown = false;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public boolean isFailOpenWhenRedisDown() {
            return failOpenWhenRedisDown;
        }

        public void setFailOpenWhenRedisDown(boolean failOpenWhenRedisDown) {
            this.failOpenWhenRedisDown = failOpenWhenRedisDown;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("*"));
        private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        private List<String> allowedHeaders = new ArrayList<>(List.of("*"));
        private List<String> exposedHeaders = new ArrayList<>(List.of("X-Request-Id", "Retry-After"));
        private boolean allowCredentials = false;
        private long maxAgeSeconds = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }
    }
}
