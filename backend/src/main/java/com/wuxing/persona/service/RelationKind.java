package com.wuxing.persona.service;

public enum RelationKind {
    DOMINANT("dominant", "主元素更清晰"),
    BALANCED("balanced", "两种气质接近");

    private final String code;
    private final String displayName;

    RelationKind(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
