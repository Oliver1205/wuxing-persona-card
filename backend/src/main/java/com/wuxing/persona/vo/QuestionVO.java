package com.wuxing.persona.vo;

import java.util.List;

public class QuestionVO {

    private String questionCode;
    private String title;
    private List<OptionVO> options;

    public QuestionVO() {
    }

    public QuestionVO(String questionCode, String title, List<OptionVO> options) {
        this.questionCode = questionCode;
        this.title = title;
        this.options = options;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<OptionVO> getOptions() {
        return options;
    }

    public void setOptions(List<OptionVO> options) {
        this.options = options;
    }
}
