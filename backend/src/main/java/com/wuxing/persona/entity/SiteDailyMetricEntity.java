package com.wuxing.persona.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SiteDailyMetricEntity {

    private Long id;
    private LocalDate metricDate;
    private long pv;
    private long uv;
    private long uip;
    private long homeViews;
    private long startClicks;
    private long testSubmits;
    private long resultCreated;
    private long shortLinkCreated;
    private long shortLinkVisits;
    private LocalDateTime aggregatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public void setMetricDate(LocalDate metricDate) {
        this.metricDate = metricDate;
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

    public long getUip() {
        return uip;
    }

    public void setUip(long uip) {
        this.uip = uip;
    }

    public long getHomeViews() {
        return homeViews;
    }

    public void setHomeViews(long homeViews) {
        this.homeViews = homeViews;
    }

    public long getStartClicks() {
        return startClicks;
    }

    public void setStartClicks(long startClicks) {
        this.startClicks = startClicks;
    }

    public long getTestSubmits() {
        return testSubmits;
    }

    public void setTestSubmits(long testSubmits) {
        this.testSubmits = testSubmits;
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

    public LocalDateTime getAggregatedAt() {
        return aggregatedAt;
    }

    public void setAggregatedAt(LocalDateTime aggregatedAt) {
        this.aggregatedAt = aggregatedAt;
    }
}
