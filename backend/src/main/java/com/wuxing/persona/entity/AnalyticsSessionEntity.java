package com.wuxing.persona.entity;

import java.time.LocalDateTime;

public class AnalyticsSessionEntity {

    private Long id;
    private String sessionIdHash;
    private String visitorIdHash;
    private LocalDateTime startedAt;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime endedAt;
    private String entryPath;
    private String latestPath;
    private String referrer;
    private String deviceType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionIdHash() {
        return sessionIdHash;
    }

    public void setSessionIdHash(String sessionIdHash) {
        this.sessionIdHash = sessionIdHash;
    }

    public String getVisitorIdHash() {
        return visitorIdHash;
    }

    public void setVisitorIdHash(String visitorIdHash) {
        this.visitorIdHash = visitorIdHash;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getEntryPath() {
        return entryPath;
    }

    public void setEntryPath(String entryPath) {
        this.entryPath = entryPath;
    }

    public String getLatestPath() {
        return latestPath;
    }

    public void setLatestPath(String latestPath) {
        this.latestPath = latestPath;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
