package com.wuxing.persona.entity;

import java.time.LocalDateTime;
import java.time.LocalDate;

public class VisitEventEntity {

    private Long id;
    private String eventType;
    private String pagePath;
    private String resultId;
    private String shortCode;
    private String clientIdHash;
    private String sessionIdHash;
    private String ipHash;
    private String userAgentHash;
    private String channel;
    private String campaign;
    private String deviceType;
    private String referer;
    private LocalDate eventDate;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getClientIdHash() {
        return clientIdHash;
    }

    public void setClientIdHash(String clientIdHash) {
        this.clientIdHash = clientIdHash;
    }

    public String getSessionIdHash() {
        return sessionIdHash;
    }

    public void setSessionIdHash(String sessionIdHash) {
        this.sessionIdHash = sessionIdHash;
    }

    public String getIpHash() {
        return ipHash;
    }

    public void setIpHash(String ipHash) {
        this.ipHash = ipHash;
    }

    public String getUserAgentHash() {
        return userAgentHash;
    }

    public void setUserAgentHash(String userAgentHash) {
        this.userAgentHash = userAgentHash;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCampaign() {
        return campaign;
    }

    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
