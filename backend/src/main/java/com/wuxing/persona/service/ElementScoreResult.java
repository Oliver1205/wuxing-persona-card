package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.Map;

public class ElementScoreResult {

    private ElementType primaryElement;
    private ElementType secondaryElement;
    private int primaryPercent;
    private int secondaryPercent;
    private Map<String, Integer> allScores;

    public ElementType getPrimaryElement() {
        return primaryElement;
    }

    public void setPrimaryElement(ElementType primaryElement) {
        this.primaryElement = primaryElement;
    }

    public ElementType getSecondaryElement() {
        return secondaryElement;
    }

    public void setSecondaryElement(ElementType secondaryElement) {
        this.secondaryElement = secondaryElement;
    }

    public int getPrimaryPercent() {
        return primaryPercent;
    }

    public void setPrimaryPercent(int primaryPercent) {
        this.primaryPercent = primaryPercent;
    }

    public int getSecondaryPercent() {
        return secondaryPercent;
    }

    public void setSecondaryPercent(int secondaryPercent) {
        this.secondaryPercent = secondaryPercent;
    }

    public Map<String, Integer> getAllScores() {
        return allScores;
    }

    public void setAllScores(Map<String, Integer> allScores) {
        this.allScores = allScores;
    }
}
