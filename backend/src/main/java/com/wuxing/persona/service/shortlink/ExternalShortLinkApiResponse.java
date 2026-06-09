package com.wuxing.persona.service.shortlink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalShortLinkApiResponse {

    private String code;
    private String message;
    private ExternalShortLinkCreateResponse data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ExternalShortLinkCreateResponse getData() {
        return data;
    }

    public void setData(ExternalShortLinkCreateResponse data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return "0".equals(code);
    }
}
