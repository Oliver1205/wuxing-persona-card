package com.wuxing.persona.vo;

public class RealtimeMetricsVO {

    private long currentOnlineVisitors;
    private long currentOnlineSessions;
    private long todayPv;
    private long todayUv;
    private long todayResults;
    private long todayShareClicks;
    private long todayMatchEnters;
    private int heartbeatIntervalSeconds;
    private int onlineWindowSeconds;
    private String refreshedAt;

    public long getCurrentOnlineVisitors() {
        return currentOnlineVisitors;
    }

    public void setCurrentOnlineVisitors(long currentOnlineVisitors) {
        this.currentOnlineVisitors = currentOnlineVisitors;
    }

    public long getCurrentOnlineSessions() {
        return currentOnlineSessions;
    }

    public void setCurrentOnlineSessions(long currentOnlineSessions) {
        this.currentOnlineSessions = currentOnlineSessions;
    }

    public long getTodayPv() {
        return todayPv;
    }

    public void setTodayPv(long todayPv) {
        this.todayPv = todayPv;
    }

    public long getTodayUv() {
        return todayUv;
    }

    public void setTodayUv(long todayUv) {
        this.todayUv = todayUv;
    }

    public long getTodayResults() {
        return todayResults;
    }

    public void setTodayResults(long todayResults) {
        this.todayResults = todayResults;
    }

    public long getTodayShareClicks() {
        return todayShareClicks;
    }

    public void setTodayShareClicks(long todayShareClicks) {
        this.todayShareClicks = todayShareClicks;
    }

    public long getTodayMatchEnters() {
        return todayMatchEnters;
    }

    public void setTodayMatchEnters(long todayMatchEnters) {
        this.todayMatchEnters = todayMatchEnters;
    }

    public int getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(int heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public int getOnlineWindowSeconds() {
        return onlineWindowSeconds;
    }

    public void setOnlineWindowSeconds(int onlineWindowSeconds) {
        this.onlineWindowSeconds = onlineWindowSeconds;
    }

    public String getRefreshedAt() {
        return refreshedAt;
    }

    public void setRefreshedAt(String refreshedAt) {
        this.refreshedAt = refreshedAt;
    }
}
