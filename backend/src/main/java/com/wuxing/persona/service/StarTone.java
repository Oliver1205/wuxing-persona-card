package com.wuxing.persona.service;

import java.util.List;

final class StarTone {

    private final String personaTypeId;
    private final String starToneName;
    private final String starToneLabel;
    private final String structureTitle;
    private final String heroSummary;
    private final String identityLine;
    private final String starToneExplanation;
    private final List<String> keywords;
    private final String internalRationale;
    private final boolean trueZiweiChart;

    StarTone(String personaTypeId,
             String starToneName,
             String starToneLabel,
             String structureTitle,
             String heroSummary,
             String identityLine,
             String starToneExplanation,
             List<String> keywords,
             String internalRationale,
             boolean trueZiweiChart) {
        this.personaTypeId = personaTypeId;
        this.starToneName = starToneName;
        this.starToneLabel = starToneLabel;
        this.structureTitle = structureTitle;
        this.heroSummary = heroSummary;
        this.identityLine = identityLine;
        this.starToneExplanation = starToneExplanation;
        this.keywords = List.copyOf(keywords);
        this.internalRationale = internalRationale;
        this.trueZiweiChart = trueZiweiChart;
    }

    String getPersonaTypeId() {
        return personaTypeId;
    }

    String getStarToneName() {
        return starToneName;
    }

    String getStarToneLabel() {
        return starToneLabel;
    }

    String getStructureTitle() {
        return structureTitle;
    }

    String getHeroSummary() {
        return heroSummary;
    }

    String getIdentityLine() {
        return identityLine;
    }

    String getStarToneExplanation() {
        return starToneExplanation;
    }

    List<String> getKeywords() {
        return keywords;
    }

    String getInternalRationale() {
        return internalRationale;
    }

    boolean isTrueZiweiChart() {
        return trueZiweiChart;
    }
}
