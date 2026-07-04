package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.List;

public class PersonaArchetype {

    private final String personaTypeId;
    private final ElementType primaryElement;
    private final ElementType secondaryElement;
    private final ElementType accentElement;
    private final RelationKind relationKind;
    private final String personaLabel;
    private final List<String> keywords;
    private final String dayMasterFrame;
    private final String primarySecondaryText;
    private final String accentText;
    private final String heavenText;
    private final String humanText;
    private final List<GrowthAdvice> growthAdvice;

    public PersonaArchetype(String personaTypeId,
                            ElementType primaryElement,
                            ElementType secondaryElement,
                            ElementType accentElement,
                            RelationKind relationKind,
                            String personaLabel,
                            List<String> keywords,
                            String dayMasterFrame,
                            String primarySecondaryText,
                            String accentText,
                            String heavenText,
                            String humanText,
                            List<GrowthAdvice> growthAdvice) {
        this.personaTypeId = personaTypeId;
        this.primaryElement = primaryElement;
        this.secondaryElement = secondaryElement;
        this.accentElement = accentElement;
        this.relationKind = relationKind;
        this.personaLabel = personaLabel;
        this.keywords = List.copyOf(keywords);
        this.dayMasterFrame = dayMasterFrame;
        this.primarySecondaryText = primarySecondaryText;
        this.accentText = accentText;
        this.heavenText = heavenText;
        this.humanText = humanText;
        this.growthAdvice = List.copyOf(growthAdvice);
    }

    public String getPersonaTypeId() {
        return personaTypeId;
    }

    public ElementType getPrimaryElement() {
        return primaryElement;
    }

    public ElementType getSecondaryElement() {
        return secondaryElement;
    }

    public ElementType getAccentElement() {
        return accentElement;
    }

    public RelationKind getRelationKind() {
        return relationKind;
    }

    public String getPersonaLabel() {
        return personaLabel;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getDayMasterFrame() {
        return dayMasterFrame;
    }

    public String getPrimarySecondaryText() {
        return primarySecondaryText;
    }

    public String getAccentText() {
        return accentText;
    }

    public String getHeavenText() {
        return heavenText;
    }

    public String getHumanText() {
        return humanText;
    }

    public List<GrowthAdvice> getGrowthAdvice() {
        return growthAdvice;
    }
}
