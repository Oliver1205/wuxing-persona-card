package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class ShortLinkVisitVO {

    private LocalDateTime createdAt;
    private String eventType;
    private String clientIdHash;
    private String ipHash;
    private String userAgentHash;
    private String referer;
    private String statSource;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getClientIdHash() {
        return clientIdHash;
    }

    public void setClientIdHash(String clientIdHash) {
        this.clientIdHash = clientIdHash;
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

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getStatSource() {
        return statSource;
    }

    public void setStatSource(String statSource) {
        this.statSource = statSource;
    }
}
