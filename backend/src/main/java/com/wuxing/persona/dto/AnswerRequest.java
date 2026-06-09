package com.wuxing.persona.dto;

import jakarta.validation.constraints.NotBlank;

public class AnswerRequest {

    @NotBlank
    private String questionCode;

    @NotBlank
    private String optionCode;

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }
}
