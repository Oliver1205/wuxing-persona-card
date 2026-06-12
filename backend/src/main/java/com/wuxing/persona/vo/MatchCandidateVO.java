package com.wuxing.persona.vo;

import java.time.LocalDateTime;
import java.util.List;

public class MatchCandidateVO {

    private String shortCode;
    private String resultId;
    private String displayName;
    private String primaryElementName;
    private String secondaryElementName;
    private List<String> keywords;
    private LocalDateTime createdAt;

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrimaryElementName() {
        return primaryElementName;
    }

    public void setPrimaryElementName(String primaryElementName) {
        this.primaryElementName = primaryElementName;
    }

    public String getSecondaryElementName() {
        return secondaryElementName;
    }

    public void setSecondaryElementName(String secondaryElementName) {
        this.secondaryElementName = secondaryElementName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
