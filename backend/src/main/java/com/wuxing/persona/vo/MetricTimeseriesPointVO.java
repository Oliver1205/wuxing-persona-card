package com.wuxing.persona.vo;

public class MetricTimeseriesPointVO {

    private String time;
    private long onlineVisitors;
    private long onlineSessions;
    private long pv;
    private long uv;
    private long resultGenerated;
    private long shareClicks;
    private long matchEnters;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getOnlineVisitors() {
        return onlineVisitors;
    }

    public void setOnlineVisitors(long onlineVisitors) {
        this.onlineVisitors = onlineVisitors;
    }

    public long getOnlineSessions() {
        return onlineSessions;
    }

    public void setOnlineSessions(long onlineSessions) {
        this.onlineSessions = onlineSessions;
    }

    public long getPv() {
        return pv;
    }

    public void setPv(long pv) {
        this.pv = pv;
    }

    public long getUv() {
        return uv;
    }

    public void setUv(long uv) {
        this.uv = uv;
    }

    public long getResultGenerated() {
        return resultGenerated;
    }

    public void setResultGenerated(long resultGenerated) {
        this.resultGenerated = resultGenerated;
    }

    public long getShareClicks() {
        return shareClicks;
    }

    public void setShareClicks(long shareClicks) {
        this.shareClicks = shareClicks;
    }

    public long getMatchEnters() {
        return matchEnters;
    }

    public void setMatchEnters(long matchEnters) {
        this.matchEnters = matchEnters;
    }
}
