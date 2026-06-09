package com.wuxing.persona.vo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ResultDetailVO {

    private String resultId;
    private String primaryElement;
    private String primaryElementName;
    private Integer primaryPercent;
    private String secondaryElement;
    private String secondaryElementName;
    private Integer secondaryPercent;
    private Map<String, Integer> allElementScores;
    private String starOfficerCode;
    private String starOfficerName;
    private List<String> keywords;
    private String layoutExplanation;
    private String strengthText;
    private String relationshipText;
    private String cardImageKey;
    private String shortCode;
    private String shortUrl;
    private LocalDateTime createdAt;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getPrimaryElement() {
        return primaryElement;
    }

    public void setPrimaryElement(String primaryElement) {
        this.primaryElement = primaryElement;
    }

    public String getPrimaryElementName() {
        return primaryElementName;
    }

    public void setPrimaryElementName(String primaryElementName) {
        this.primaryElementName = primaryElementName;
    }

    public Integer getPrimaryPercent() {
        return primaryPercent;
    }

    public void setPrimaryPercent(Integer primaryPercent) {
        this.primaryPercent = primaryPercent;
    }

    public String getSecondaryElement() {
        return secondaryElement;
    }

    public void setSecondaryElement(String secondaryElement) {
        this.secondaryElement = secondaryElement;
    }

    public String getSecondaryElementName() {
        return secondaryElementName;
    }

    public void setSecondaryElementName(String secondaryElementName) {
        this.secondaryElementName = secondaryElementName;
    }

    public Integer getSecondaryPercent() {
        return secondaryPercent;
    }

    public void setSecondaryPercent(Integer secondaryPercent) {
        this.secondaryPercent = secondaryPercent;
    }

    public Map<String, Integer> getAllElementScores() {
        return allElementScores;
    }

    public void setAllElementScores(Map<String, Integer> allElementScores) {
        this.allElementScores = allElementScores;
    }

    public String getStarOfficerCode() {
        return starOfficerCode;
    }

    public void setStarOfficerCode(String starOfficerCode) {
        this.starOfficerCode = starOfficerCode;
    }

    public String getStarOfficerName() {
        return starOfficerName;
    }

    public void setStarOfficerName(String starOfficerName) {
        this.starOfficerName = starOfficerName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getLayoutExplanation() {
        return layoutExplanation;
    }

    public void setLayoutExplanation(String layoutExplanation) {
        this.layoutExplanation = layoutExplanation;
    }

    public String getStrengthText() {
        return strengthText;
    }

    public void setStrengthText(String strengthText) {
        this.strengthText = strengthText;
    }

    public String getRelationshipText() {
        return relationshipText;
    }

    public void setRelationshipText(String relationshipText) {
        this.relationshipText = relationshipText;
    }

    public String getCardImageKey() {
        return cardImageKey;
    }

    public void setCardImageKey(String cardImageKey) {
        this.cardImageKey = cardImageKey;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
