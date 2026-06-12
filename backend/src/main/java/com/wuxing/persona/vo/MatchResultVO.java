package com.wuxing.persona.vo;

import java.time.LocalDateTime;
import java.util.List;

public class MatchResultVO {

    private String matchId;
    private String partnerShortCode;
    private String currentShortCode;
    private ResultDetailVO partnerResult;
    private ResultDetailVO currentResult;
    private Integer compatibilityScore;
    private String relationLabel;
    private String headline;
    private String summary;
    private List<String> strengths;
    private List<String> suggestions;
    private LocalDateTime createdAt;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPartnerShortCode() {
        return partnerShortCode;
    }

    public void setPartnerShortCode(String partnerShortCode) {
        this.partnerShortCode = partnerShortCode;
    }

    public String getCurrentShortCode() {
        return currentShortCode;
    }

    public void setCurrentShortCode(String currentShortCode) {
        this.currentShortCode = currentShortCode;
    }

    public ResultDetailVO getPartnerResult() {
        return partnerResult;
    }

    public void setPartnerResult(ResultDetailVO partnerResult) {
        this.partnerResult = partnerResult;
    }

    public ResultDetailVO getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(ResultDetailVO currentResult) {
        this.currentResult = currentResult;
    }

    public Integer getCompatibilityScore() {
        return compatibilityScore;
    }

    public void setCompatibilityScore(Integer compatibilityScore) {
        this.compatibilityScore = compatibilityScore;
    }

    public String getRelationLabel() {
        return relationLabel;
    }

    public void setRelationLabel(String relationLabel) {
        this.relationLabel = relationLabel;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
