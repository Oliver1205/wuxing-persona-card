package com.wuxing.persona.vo;

public class AnalyticsAggregationVO {

    private String startDate;
    private String endDate;
    private int daysAggregated;
    private long shortLinkRowsAggregated;
    private String aggregatedAt;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getDaysAggregated() {
        return daysAggregated;
    }

    public void setDaysAggregated(int daysAggregated) {
        this.daysAggregated = daysAggregated;
    }

    public long getShortLinkRowsAggregated() {
        return shortLinkRowsAggregated;
    }

    public void setShortLinkRowsAggregated(long shortLinkRowsAggregated) {
        this.shortLinkRowsAggregated = shortLinkRowsAggregated;
    }

    public String getAggregatedAt() {
        return aggregatedAt;
    }

    public void setAggregatedAt(String aggregatedAt) {
        this.aggregatedAt = aggregatedAt;
    }
}
