package com.wuxing.persona.service.shortlink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalShortLinkStatsApiResponse {

    private String code;
    private String message;
    private ExternalShortLinkStatsResponse data;

    public boolean isSuccess() {
        return "0".equals(code);
    }

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

    public ExternalShortLinkStatsResponse getData() {
        return data;
    }

    public void setData(ExternalShortLinkStatsResponse data) {
        this.data = data;
    }
}
