package com.wuxing.persona.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ShortLinkDailyMetricEntity {

    private Long id;
    private LocalDate metricDate;
    private String shortCode;
    private long pv;
    private long uv;
    private long uip;
    private LocalDateTime lastVisitAt;
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

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
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

    public LocalDateTime getLastVisitAt() {
        return lastVisitAt;
    }

    public void setLastVisitAt(LocalDateTime lastVisitAt) {
        this.lastVisitAt = lastVisitAt;
    }

    public LocalDateTime getAggregatedAt() {
        return aggregatedAt;
    }

    public void setAggregatedAt(LocalDateTime aggregatedAt) {
        this.aggregatedAt = aggregatedAt;
    }
}
