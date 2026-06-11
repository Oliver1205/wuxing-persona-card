package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class ShortLinkVisitVO {

    private LocalDateTime createdAt;
    private String eventType;
    private String clientIdHash;
    private String ipHash;
    private String userAgentHash;
    private String channel;
    private String campaign;
    private String deviceType;
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

    public String getStatSource() {
        return statSource;
    }

    public void setStatSource(String statSource) {
        this.statSource = statSource;
    }
}
