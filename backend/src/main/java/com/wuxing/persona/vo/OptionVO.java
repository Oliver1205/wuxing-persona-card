package com.wuxing.persona.vo;

public class OptionVO {

    private String optionCode;
    private String optionText;
    private String elementCode;
    private String elementName;

    public OptionVO() {
    }

    public OptionVO(String optionCode, String optionText, String elementCode, String elementName) {
        this.optionCode = optionCode;
        this.optionText = optionText;
        this.elementCode = elementCode;
        this.elementName = elementName;
    }

    public String getOptionCode() {
        return optionCode;
    }

    public void setOptionCode(String optionCode) {
        this.optionCode = optionCode;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public String getElementCode() {
        return elementCode;
    }

    public void setElementCode(String elementCode) {
        this.elementCode = elementCode;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }
}
