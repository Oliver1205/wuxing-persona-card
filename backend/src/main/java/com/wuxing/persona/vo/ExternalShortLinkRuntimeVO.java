package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class ExternalShortLinkRuntimeVO {

    private String mode;
    private boolean externalMode;
    private boolean statsEnabled;
    private boolean fallbackToInternal;
    private String baseUrl;
    private String domain;
    private String groupId;
    private Boolean reachable;
    private Integer httpStatus;
    private String message;
    private LocalDateTime checkedAt;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isExternalMode() {
        return externalMode;
    }

    public void setExternalMode(boolean externalMode) {
        this.externalMode = externalMode;
    }

    public boolean isStatsEnabled() {
        return statsEnabled;
    }

    public void setStatsEnabled(boolean statsEnabled) {
        this.statsEnabled = statsEnabled;
    }

    public boolean isFallbackToInternal() {
        return fallbackToInternal;
    }

    public void setFallbackToInternal(boolean fallbackToInternal) {
        this.fallbackToInternal = fallbackToInternal;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Boolean getReachable() {
        return reachable;
    }

    public void setReachable(Boolean reachable) {
        this.reachable = reachable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
}
