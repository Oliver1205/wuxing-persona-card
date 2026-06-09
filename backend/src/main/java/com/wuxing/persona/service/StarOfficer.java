package com.wuxing.persona.service;

import com.wuxing.persona.enums.ElementType;
import java.util.List;

public class StarOfficer {

    private String code;
    private String name;
    private ElementType elementType;
    private List<String> traits;

    public StarOfficer(String code, String name, ElementType elementType, List<String> traits) {
        this.code = code;
        this.name = name;
        this.elementType = elementType;
        this.traits = traits;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public List<String> getTraits() {
        return traits;
    }
}
