package com.wuxing.persona.entity;

import java.time.LocalDateTime;

public class AnalyticsMetricSnapshotEntity {

    private Long id;
    private LocalDateTime metricTime;
    private long onlineVisitors;
    private long onlineSessions;
    private long pv1m;
    private long uv1m;
    private long resultGenerated1m;
    private long shareClick1m;
    private long matchEnter1m;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getMetricTime() {
        return metricTime;
    }

    public void setMetricTime(LocalDateTime metricTime) {
        this.metricTime = metricTime;
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

    public long getPv1m() {
        return pv1m;
    }

    public void setPv1m(long pv1m) {
        this.pv1m = pv1m;
    }

    public long getUv1m() {
        return uv1m;
    }

    public void setUv1m(long uv1m) {
        this.uv1m = uv1m;
    }

    public long getResultGenerated1m() {
        return resultGenerated1m;
    }

    public void setResultGenerated1m(long resultGenerated1m) {
        this.resultGenerated1m = resultGenerated1m;
    }

    public long getShareClick1m() {
        return shareClick1m;
    }

    public void setShareClick1m(long shareClick1m) {
        this.shareClick1m = shareClick1m;
    }

    public long getMatchEnter1m() {
        return matchEnter1m;
    }

    public void setMatchEnter1m(long matchEnter1m) {
        this.matchEnter1m = matchEnter1m;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
