package com.wuxing.persona.vo;

import java.util.List;

public class AdminOverviewVO {

    private long totalPv;
    private long totalUv;
    private long totalUip;
    private long homeViews;
    private long startClicks;
    private long testSubmits;
    private long resultCreated;
    private long shortLinkCreated;
    private long shortLinkVisits;
    private double completionRate;
    private List<DailyMetricVO> dailyTrends;
    private List<NameCountVO> popularElementCombos;
    private List<NameCountVO> popularStarOfficers;
    private List<RecentResultVO> recentResults;
    private List<ShortLinkListItemVO> recentShortLinks;

    public long getTotalPv() {
        return totalPv;
    }

    public void setTotalPv(long totalPv) {
        this.totalPv = totalPv;
    }

    public long getTotalUv() {
        return totalUv;
    }

    public void setTotalUv(long totalUv) {
        this.totalUv = totalUv;
    }

    public long getTotalUip() {
        return totalUip;
    }

    public void setTotalUip(long totalUip) {
        this.totalUip = totalUip;
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

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public List<DailyMetricVO> getDailyTrends() {
        return dailyTrends;
    }

    public void setDailyTrends(List<DailyMetricVO> dailyTrends) {
        this.dailyTrends = dailyTrends;
    }

    public List<NameCountVO> getPopularElementCombos() {
        return popularElementCombos;
    }

    public void setPopularElementCombos(List<NameCountVO> popularElementCombos) {
        this.popularElementCombos = popularElementCombos;
    }

    public List<NameCountVO> getPopularStarOfficers() {
        return popularStarOfficers;
    }

    public void setPopularStarOfficers(List<NameCountVO> popularStarOfficers) {
        this.popularStarOfficers = popularStarOfficers;
    }

    public List<RecentResultVO> getRecentResults() {
        return recentResults;
    }

    public void setRecentResults(List<RecentResultVO> recentResults) {
        this.recentResults = recentResults;
    }

    public List<ShortLinkListItemVO> getRecentShortLinks() {
        return recentShortLinks;
    }

    public void setRecentShortLinks(List<ShortLinkListItemVO> recentShortLinks) {
        this.recentShortLinks = recentShortLinks;
    }
}
