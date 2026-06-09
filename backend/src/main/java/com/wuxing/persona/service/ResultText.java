package com.wuxing.persona.service;

import java.util.List;

public class ResultText {

    private List<String> keywords;
    private String layoutExplanation;
    private String strengthText;
    private String relationshipText;

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
}
