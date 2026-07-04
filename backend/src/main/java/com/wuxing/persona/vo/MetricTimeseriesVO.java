package com.wuxing.persona.vo;

import java.util.List;

public class MetricTimeseriesVO {

    private String range;
    private int intervalSeconds;
    private String generatedAt;
    private List<MetricTimeseriesPointVO> points;

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public List<MetricTimeseriesPointVO> getPoints() {
        return points;
    }

    public void setPoints(List<MetricTimeseriesPointVO> points) {
        this.points = points;
    }
}
