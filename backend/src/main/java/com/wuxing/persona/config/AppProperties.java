package com.wuxing.persona.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private String adminToken;
    private String hashSalt;
    private CorsProperties cors = new CorsProperties();
    private ShortLinkProperties shortLink = new ShortLinkProperties();
    private VisitEventProperties visitEvent = new VisitEventProperties();
    private AnalyticsProperties analytics = new AnalyticsProperties();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = trimTrailingSlash(baseUrl);
    }

    public String getAdminToken() {
        return adminToken;
    }

    public void setAdminToken(String adminToken) {
        this.adminToken = adminToken;
    }

    public String getHashSalt() {
        return hashSalt;
    }

    public void setHashSalt(String hashSalt) {
        this.hashSalt = hashSalt;
    }

    public CorsProperties getCors() {
        return cors;
    }

    public void setCors(CorsProperties cors) {
        if (cors != null) {
            this.cors = cors;
        }
    }

    public ShortLinkProperties getShortLink() {
        return shortLink;
    }

    public void setShortLink(ShortLinkProperties shortLink) {
        if (shortLink != null) {
            this.shortLink = shortLink;
        }
    }

    public VisitEventProperties getVisitEvent() {
        return visitEvent;
    }

    public void setVisitEvent(VisitEventProperties visitEvent) {
        if (visitEvent != null) {
            this.visitEvent = visitEvent;
        }
    }

    public AnalyticsProperties getAnalytics() {
        return analytics;
    }

    public void setAnalytics(AnalyticsProperties analytics) {
        if (analytics != null) {
            this.analytics = analytics;
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static class CorsProperties {

        private List<String> allowedOrigins = new ArrayList<>();
        private long maxAgeSeconds = 3600;

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            if (allowedOrigins == null) {
                this.allowedOrigins = new ArrayList<>();
                return;
            }
            this.allowedOrigins = allowedOrigins.stream()
                    .map(origin -> origin == null ? "" : origin.trim())
                    .filter(origin -> !origin.isBlank())
                    .distinct()
                    .toList();
        }

        public long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public void setMaxAgeSeconds(long maxAgeSeconds) {
            this.maxAgeSeconds = Math.max(0, maxAgeSeconds);
        }
    }

    public static class ShortLinkProperties {

        private String mode = "internal";
        private int lastVisitTouchIntervalSeconds = 30;
        private ExternalShortLinkProperties external = new ExternalShortLinkProperties();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public int getLastVisitTouchIntervalSeconds() {
            return lastVisitTouchIntervalSeconds;
        }

        public void setLastVisitTouchIntervalSeconds(int lastVisitTouchIntervalSeconds) {
            this.lastVisitTouchIntervalSeconds = Math.max(0, lastVisitTouchIntervalSeconds);
        }

        public ExternalShortLinkProperties getExternal() {
            return external;
        }

        public void setExternal(ExternalShortLinkProperties external) {
            if (external != null) {
                this.external = external;
            }
        }
    }

    public static class ExternalShortLinkProperties {

        private String baseUrl = "http://localhost:8003";
        private String groupId = "wuxing_persona";
        private String domain = "nurl.ink:8003";
        private boolean fallbackToInternal = true;
        private String systemUsername = "wuxing_system";
        private String systemUserId = "wuxing-system";
        private String systemRealName = "wuxing-system";
        private int connectTimeoutMillis = 2000;
        private int readTimeoutMillis = 3000;
        private boolean statsEnabled = false;
        private int statsEnableStatus = 0;
        private int statsCacheTtlSeconds = 60;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = trimTrailingSlashValue(baseUrl, "http://localhost:8003");
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getDomain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public boolean isFallbackToInternal() {
            return fallbackToInternal;
        }

        public void setFallbackToInternal(boolean fallbackToInternal) {
            this.fallbackToInternal = fallbackToInternal;
        }

        public String getSystemUsername() {
            return systemUsername;
        }

        public void setSystemUsername(String systemUsername) {
            this.systemUsername = systemUsername;
        }

        public String getSystemUserId() {
            return systemUserId;
        }

        public void setSystemUserId(String systemUserId) {
            this.systemUserId = systemUserId;
        }

        public String getSystemRealName() {
            return systemRealName;
        }

        public void setSystemRealName(String systemRealName) {
            this.systemRealName = systemRealName;
        }

        public int getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public int getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(int readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public boolean isStatsEnabled() {
            return statsEnabled;
        }

        public void setStatsEnabled(boolean statsEnabled) {
            this.statsEnabled = statsEnabled;
        }

        public int getStatsEnableStatus() {
            return statsEnableStatus;
        }

        public void setStatsEnableStatus(int statsEnableStatus) {
            this.statsEnableStatus = statsEnableStatus;
        }

        public int getStatsCacheTtlSeconds() {
            return statsCacheTtlSeconds;
        }

        public void setStatsCacheTtlSeconds(int statsCacheTtlSeconds) {
            this.statsCacheTtlSeconds = Math.max(0, statsCacheTtlSeconds);
        }

        private static String trimTrailingSlashValue(String value, String defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            while (value.endsWith("/")) {
                value = value.substring(0, value.length() - 1);
            }
            return value;
        }
    }

    public static class VisitEventProperties {

        private int asyncQueueCapacity = 2048;
        private int asyncDrainLimit = 64;
        private String asyncMode = "local";
        private RocketMqProperties rocketmq = new RocketMqProperties();

        public int getAsyncQueueCapacity() {
            return asyncQueueCapacity;
        }

        public void setAsyncQueueCapacity(int asyncQueueCapacity) {
            this.asyncQueueCapacity = Math.max(1, asyncQueueCapacity);
        }

        public int getAsyncDrainLimit() {
            return asyncDrainLimit;
        }

        public void setAsyncDrainLimit(int asyncDrainLimit) {
            this.asyncDrainLimit = Math.max(1, asyncDrainLimit);
        }

        public String getAsyncMode() {
            return asyncMode;
        }

        public void setAsyncMode(String asyncMode) {
            if (asyncMode == null || asyncMode.isBlank()) {
                this.asyncMode = "local";
                return;
            }
            String normalized = asyncMode.trim().toLowerCase(Locale.ROOT);
            if (!"local".equals(normalized) && !"rocketmq".equals(normalized) && !"sync".equals(normalized)) {
                throw new IllegalArgumentException("app.visit-event.async-mode must be local, rocketmq or sync");
            }
            this.asyncMode = normalized;
        }

        public boolean isSyncMode() {
            return "sync".equalsIgnoreCase(asyncMode);
        }

        public boolean isRocketMqMode() {
            return "rocketmq".equalsIgnoreCase(asyncMode);
        }

        public RocketMqProperties getRocketmq() {
            return rocketmq;
        }

        public void setRocketmq(RocketMqProperties rocketmq) {
            if (rocketmq != null) {
                this.rocketmq = rocketmq;
            }
        }
    }

    public static class RocketMqProperties {

        private String nameServer = "localhost:9876";
        private String topic = "wuxing-visit-event";
        private String tag = "visit";
        private String producerGroup = "wuxing-persona-producer";
        private String consumerGroup = "wuxing-persona-consumer";
        private int publishTimeoutMillis = 1000;
        private boolean fallbackToLocal = true;
        private boolean consumerEnabled = false;

        public String getNameServer() {
            return nameServer;
        }

        public void setNameServer(String nameServer) {
            this.nameServer = defaultIfBlank(nameServer, "localhost:9876");
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = defaultIfBlank(topic, "wuxing-visit-event");
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = defaultIfBlank(tag, "visit");
        }

        public String getProducerGroup() {
            return producerGroup;
        }

        public void setProducerGroup(String producerGroup) {
            this.producerGroup = defaultIfBlank(producerGroup, "wuxing-persona-producer");
        }

        public String getConsumerGroup() {
            return consumerGroup;
        }

        public void setConsumerGroup(String consumerGroup) {
            this.consumerGroup = defaultIfBlank(consumerGroup, "wuxing-persona-consumer");
        }

        public int getPublishTimeoutMillis() {
            return publishTimeoutMillis;
        }

        public void setPublishTimeoutMillis(int publishTimeoutMillis) {
            this.publishTimeoutMillis = Math.max(100, publishTimeoutMillis);
        }

        public boolean isFallbackToLocal() {
            return fallbackToLocal;
        }

        public void setFallbackToLocal(boolean fallbackToLocal) {
            this.fallbackToLocal = fallbackToLocal;
        }

        public boolean isConsumerEnabled() {
            return consumerEnabled;
        }

        public void setConsumerEnabled(boolean consumerEnabled) {
            this.consumerEnabled = consumerEnabled;
        }

        private String defaultIfBlank(String value, String defaultValue) {
            return value == null || value.isBlank() ? defaultValue : value.trim();
        }
    }

    public static class AnalyticsProperties {

        private boolean enabled = true;
        private int heartbeatIntervalMillis = 30000;
        private int onlineWindowMillis = 120000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getHeartbeatIntervalMillis() {
            return heartbeatIntervalMillis;
        }

        public void setHeartbeatIntervalMillis(int heartbeatIntervalMillis) {
            this.heartbeatIntervalMillis = Math.max(10000, heartbeatIntervalMillis);
        }

        public int getOnlineWindowMillis() {
            return onlineWindowMillis;
        }

        public void setOnlineWindowMillis(int onlineWindowMillis) {
            this.onlineWindowMillis = Math.max(heartbeatIntervalMillis * 2, onlineWindowMillis);
        }
    }
}
