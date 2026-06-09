package com.wuxing.persona.entity;

import java.time.LocalDateTime;

public class UserResultEntity {

    private Long id;
    private String resultId;
    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private String birthTimeRange;
    private String answerJson;
    private String primaryElement;
    private String secondaryElement;
    private Integer primaryPercent;
    private Integer secondaryPercent;
    private String allElementScoresJson;
    private String starOfficerCode;
    private String starOfficerName;
    private String keywordsJson;
    private String layoutExplanation;
    private String strengthText;
    private String relationshipText;
    private String cardImageKey;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(Integer birthMonth) {
        this.birthMonth = birthMonth;
    }

    public Integer getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Integer birthDay) {
        this.birthDay = birthDay;
    }

    public String getBirthTimeRange() {
        return birthTimeRange;
    }

    public void setBirthTimeRange(String birthTimeRange) {
        this.birthTimeRange = birthTimeRange;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
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

    public Integer getPrimaryPercent() {
        return primaryPercent;
    }

    public void setPrimaryPercent(Integer primaryPercent) {
        this.primaryPercent = primaryPercent;
    }

    public Integer getSecondaryPercent() {
        return secondaryPercent;
    }

    public void setSecondaryPercent(Integer secondaryPercent) {
        this.secondaryPercent = secondaryPercent;
    }

    public String getAllElementScoresJson() {
        return allElementScoresJson;
    }

    public void setAllElementScoresJson(String allElementScoresJson) {
        this.allElementScoresJson = allElementScoresJson;
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

    public String getKeywordsJson() {
        return keywordsJson;
    }

    public void setKeywordsJson(String keywordsJson) {
        this.keywordsJson = keywordsJson;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
