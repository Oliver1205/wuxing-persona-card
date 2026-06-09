package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class ShortLinkListItemVO {

    private String shortCode;
    private String shortUrl;
    private String resultId;
    private String elementCombo;
    private String starOfficerName;
    private LocalDateTime createdAt;
    private long pv;
    private long uv;
    private long uip;
    private LocalDateTime lastVisitAt;

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getElementCombo() {
        return elementCombo;
    }

    public void setElementCombo(String elementCombo) {
        this.elementCombo = elementCombo;
    }

    public String getStarOfficerName() {
        return starOfficerName;
    }

    public void setStarOfficerName(String starOfficerName) {
        this.starOfficerName = starOfficerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getPv() {
        return pv;
    }

    public void setPv(long pv) {
        this.pv = pv;
    }

    public long getUv() {
        return uv;
    }

    public void setUv(long uv) {
        this.uv = uv;
    }

    public long getUip() {
        return uip;
    }

    public void setUip(long uip) {
        this.uip = uip;
    }

    public LocalDateTime getLastVisitAt() {
        return lastVisitAt;
    }

    public void setLastVisitAt(LocalDateTime lastVisitAt) {
        this.lastVisitAt = lastVisitAt;
    }
}
