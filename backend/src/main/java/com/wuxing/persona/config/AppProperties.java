package com.wuxing.persona.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String baseUrl;
    private String adminToken;
    private String hashSalt;
    private ShortLinkProperties shortLink = new ShortLinkProperties();

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

    public ShortLinkProperties getShortLink() {
        return shortLink;
    }

    public void setShortLink(ShortLinkProperties shortLink) {
        if (shortLink != null) {
            this.shortLink = shortLink;
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

    public static class ShortLinkProperties {

        private String mode = "internal";
        private ExternalShortLinkProperties external = new ExternalShortLinkProperties();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
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
}
