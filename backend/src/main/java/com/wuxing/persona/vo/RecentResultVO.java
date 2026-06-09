package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class RecentResultVO {

    private String resultId;
    private String elementCombo;
    private String starOfficerName;
    private LocalDateTime createdAt;

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getElementCombo() {
        return elementCombo;
    }

    public void setElementCombo(String elementCombo) {
        this.elementCombo = elementCombo;
    }

    public String getStarOfficerName() {
        return starOfficerName;
    }

    public void setStarOfficerName(String starOfficerName) {
        this.starOfficerName = starOfficerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
