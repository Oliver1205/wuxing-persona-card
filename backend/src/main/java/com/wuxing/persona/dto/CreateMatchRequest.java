package com.wuxing.persona.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateMatchRequest extends CreateResultRequest {

    @NotBlank
    private String partnerShortCode;

    public String getPartnerShortCode() {
        return partnerShortCode;
    }

    public void setPartnerShortCode(String partnerShortCode) {
        this.partnerShortCode = partnerShortCode;
    }
}
