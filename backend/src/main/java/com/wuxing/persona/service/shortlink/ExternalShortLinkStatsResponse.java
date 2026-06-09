package com.wuxing.persona.service.shortlink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalShortLinkStatsResponse {

    private Long pv;
    private Long uv;
    private Long uip;

    public Long getPv() {
        return pv;
    }

    public void setPv(Long pv) {
        this.pv = pv;
    }

    public Long getUv() {
        return uv;
    }

    public void setUv(Long uv) {
        this.uv = uv;
    }

    public Long getUip() {
        return uip;
    }

    public void setUip(Long uip) {
        this.uip = uip;
    }
}
