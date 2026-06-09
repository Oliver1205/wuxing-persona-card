package com.wuxing.persona.enums;

import java.util.Arrays;

public enum BirthTimeRange {
    MORNING,
    NOON,
    AFTERNOON,
    EVENING,
    NIGHT,
    UNKNOWN;

    public static BirthTimeRange fromNullable(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(each -> each.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid birthTimeRange: " + code));
    }
}
