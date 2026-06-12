package com.wuxing.persona.enums;

import java.util.Arrays;

public enum EventType {
    PAGE_VIEW_HOME,
    START_TEST_CLICK,
    TEST_FORM_START,
    QUESTION_ANSWER_SELECT,
    TEST_SUBMIT_ATTEMPT,
    TEST_SUBMIT,
    RESULT_CREATED,
    RESULT_VIEW,
    SHORT_LINK_CREATED,
    SHORT_LINK_COPY,
    SAVE_SHARE_IMAGE_SUCCESS,
    NATIVE_SHARE_SUCCESS,
    SHARE_PANEL_VIEW,
    SHARED_RESULT_CTA_CLICK,
    RETAKE_TEST_CLICK,
    MATCH_CLIPBOARD_DETECTED,
    MATCH_SHORT_CODE_ENTERED,
    MATCH_MODE_ACCEPT,
    MATCH_MODE_DISMISS,
    MATCH_RESULT_VIEW,
    SHORT_LINK_VISIT;

    public static EventType fromCode(String code) {
        return Arrays.stream(values())
                .filter(each -> each.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid eventType: " + code));
    }
}
