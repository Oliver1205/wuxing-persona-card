package com.wuxing.persona.enums;

import java.util.Arrays;
import java.util.List;

public enum ElementType {
    METAL("金", List.of("清醒", "秩序", "判断", "执行", "边界")),
    WOOD("木", List.of("成长", "规划", "创造", "耐心", "引导")),
    WATER("水", List.of("观察", "共情", "适应", "细腻", "洞察")),
    FIRE("火", List.of("热情", "行动", "表达", "感染力", "突破")),
    EARTH("土", List.of("稳定", "承载", "协调", "可靠", "安全感"));

    private final String displayName;
    private final List<String> keywords;

    ElementType(String displayName, List<String> keywords) {
        this.displayName = displayName;
        this.keywords = keywords;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public static ElementType fromCode(String code) {
        return Arrays.stream(values())
                .filter(each -> each.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("invalid element: " + code));
    }
}
