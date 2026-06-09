package com.wuxing.persona.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class CreateResultRequest {

    @NotNull
    @Min(1900)
    private Integer birthYear;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer birthMonth;

    @Min(1)
    @Max(31)
    private Integer birthDay;

    private String birthTimeRange;

    @Valid
    @NotNull
    @Size(min = 5, max = 5)
    private List<AnswerRequest> answers;

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getBirthMonth() {
        return birthMonth;
    }

    public void setBirthMonth(Integer birthMonth) {
        this.birthMonth = birthMonth;
    }

    public Integer getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(Integer birthDay) {
        this.birthDay = birthDay;
    }

    public String getBirthTimeRange() {
        return birthTimeRange;
    }

    public void setBirthTimeRange(String birthTimeRange) {
        this.birthTimeRange = birthTimeRange;
    }

    public List<AnswerRequest> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerRequest> answers) {
        this.answers = answers;
    }
}
