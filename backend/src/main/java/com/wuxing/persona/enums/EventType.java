package com.wuxing.persona.enums;

import java.util.Arrays;

public enum EventType {
    PAGE_VIEW_HOME,
    START_TEST_CLICK,
    TEST_SUBMIT,
    RESULT_CREATED,
    RESULT_VIEW,
    SHORT_LINK_CREATED,
    SHORT_LINK_COPY,
    SHORT_LINK_VISIT;

    public static EventType fromCode(String code) {
        return Arrays.stream(values())
                .filter(each -> each.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid eventType: " + code));
    }
}
