package com.wuxing.persona.service.shortlink;

public class ExternalShortLinkStatsSnapshot {

    private final long pv;
    private final long uv;
    private final long uip;

    public ExternalShortLinkStatsSnapshot(long pv, long uv, long uip) {
        this.pv = pv;
        this.uv = uv;
        this.uip = uip;
    }

    public long getPv() {
        return pv;
    }

    public long getUv() {
        return uv;
    }

    public long getUip() {
        return uip;
    }
}
