package com.wuxing.persona.dto;

public class AnalyticsEventRequest {

    private String eventName;
    private String path;
    private String resultId;
    private String shortCode;
    private String visitorId;
    private String sessionId;
    private String starToneName;
    private String primaryElement;
    private String secondaryElement;
    private String accentElement;
    private Boolean success;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStarToneName() {
        return starToneName;
    }

    public void setStarToneName(String starToneName) {
        this.starToneName = starToneName;
    }

    public String getPrimaryElement() {
        return primaryElement;
    }

    public void setPrimaryElement(String primaryElement) {
        this.primaryElement = primaryElement;
    }

    public String getSecondaryElement() {
        return secondaryElement;
    }

    public void setSecondaryElement(String secondaryElement) {
        this.secondaryElement = secondaryElement;
    }

    public String getAccentElement() {
        return accentElement;
    }

    public void setAccentElement(String accentElement) {
        this.accentElement = accentElement;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }
}
