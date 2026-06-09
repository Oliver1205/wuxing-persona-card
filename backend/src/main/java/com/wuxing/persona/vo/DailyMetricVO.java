package com.wuxing.persona.vo;

public class DailyMetricVO {

    private String date;
    private long pv;
    private long resultCreated;
    private long shortLinkCreated;
    private long shortLinkVisits;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getPv() {
        return pv;
    }

    public void setPv(long pv) {
        this.pv = pv;
    }

    public long getResultCreated() {
        return resultCreated;
    }

    public void setResultCreated(long resultCreated) {
        this.resultCreated = resultCreated;
    }

    public long getShortLinkCreated() {
        return shortLinkCreated;
    }

    public void setShortLinkCreated(long shortLinkCreated) {
        this.shortLinkCreated = shortLinkCreated;
    }

    public long getShortLinkVisits() {
        return shortLinkVisits;
    }

    public void setShortLinkVisits(long shortLinkVisits) {
        this.shortLinkVisits = shortLinkVisits;
    }
}
