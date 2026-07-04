package com.wuxing.persona.vo;

import com.wuxing.persona.service.GrowthAdvice;
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
    private String personaTypeId;
    private String accentElement;
    private String accentElementName;
    private String relationKind;
    private String personaLabel;
    private String starToneName;
    private String starToneLabel;
    private String structureTitle;
    private String heroSummary;
    private String identityLine;
    private String starToneExplanation;
    private String dayMasterText;
    private String primarySecondaryText;
    private String accentText;
    private String heavenText;
    private String humanText;
    private String starOfficerText;
    private List<GrowthAdvice> growthAdvice;
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

    public String getPersonaTypeId() {
        return personaTypeId;
    }

    public void setPersonaTypeId(String personaTypeId) {
        this.personaTypeId = personaTypeId;
    }

    public String getAccentElement() {
        return accentElement;
    }

    public void setAccentElement(String accentElement) {
        this.accentElement = accentElement;
    }

    public String getAccentElementName() {
        return accentElementName;
    }

    public void setAccentElementName(String accentElementName) {
        this.accentElementName = accentElementName;
    }

    public String getRelationKind() {
        return relationKind;
    }

    public void setRelationKind(String relationKind) {
        this.relationKind = relationKind;
    }

    public String getPersonaLabel() {
        return personaLabel;
    }

    public void setPersonaLabel(String personaLabel) {
        this.personaLabel = personaLabel;
    }

    public String getStarToneName() {
        return starToneName;
    }

    public void setStarToneName(String starToneName) {
        this.starToneName = starToneName;
    }

    public String getStarToneLabel() {
        return starToneLabel;
    }

    public void setStarToneLabel(String starToneLabel) {
        this.starToneLabel = starToneLabel;
    }

    public String getStructureTitle() {
        return structureTitle;
    }

    public void setStructureTitle(String structureTitle) {
        this.structureTitle = structureTitle;
    }

    public String getHeroSummary() {
        return heroSummary;
    }

    public void setHeroSummary(String heroSummary) {
        this.heroSummary = heroSummary;
    }

    public String getIdentityLine() {
        return identityLine;
    }

    public void setIdentityLine(String identityLine) {
        this.identityLine = identityLine;
    }

    public String getStarToneExplanation() {
        return starToneExplanation;
    }

    public void setStarToneExplanation(String starToneExplanation) {
        this.starToneExplanation = starToneExplanation;
    }

    public String getDayMasterText() {
        return dayMasterText;
    }

    public void setDayMasterText(String dayMasterText) {
        this.dayMasterText = dayMasterText;
    }

    public String getPrimarySecondaryText() {
        return primarySecondaryText;
    }

    public void setPrimarySecondaryText(String primarySecondaryText) {
        this.primarySecondaryText = primarySecondaryText;
    }

    public String getAccentText() {
        return accentText;
    }

    public void setAccentText(String accentText) {
        this.accentText = accentText;
    }

    public String getHeavenText() {
        return heavenText;
    }

    public void setHeavenText(String heavenText) {
        this.heavenText = heavenText;
    }

    public String getHumanText() {
        return humanText;
    }

    public void setHumanText(String humanText) {
        this.humanText = humanText;
    }

    public String getStarOfficerText() {
        return starOfficerText;
    }

    public void setStarOfficerText(String starOfficerText) {
        this.starOfficerText = starOfficerText;
    }

    public List<GrowthAdvice> getGrowthAdvice() {
        return growthAdvice;
    }

    public void setGrowthAdvice(List<GrowthAdvice> growthAdvice) {
        this.growthAdvice = growthAdvice;
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
