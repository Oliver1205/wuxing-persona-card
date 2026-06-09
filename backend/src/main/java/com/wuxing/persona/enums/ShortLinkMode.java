package com.wuxing.persona.enums;

import com.wuxing.persona.common.BusinessException;

public enum ShortLinkMode {
    INTERNAL,
    EXTERNAL;

    public static ShortLinkMode from(String value) {
        if (value == null || value.isBlank()) {
            return INTERNAL;
        }
        for (ShortLinkMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        throw new BusinessException("short link mode must be internal or external");
    }
}
