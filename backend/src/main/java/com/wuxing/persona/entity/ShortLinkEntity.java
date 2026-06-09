package com.wuxing.persona.entity;

import java.time.LocalDateTime;

public class ShortLinkEntity {

    private Long id;
    private String shortCode;
    private String resultId;
    private String originalPath;
    private String shortUrl;
    private Long pvCount;
    private Long uvCount;
    private Long uipCount;
    private LocalDateTime lastVisitAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public Long getPvCount() {
        return pvCount;
    }

    public void setPvCount(Long pvCount) {
        this.pvCount = pvCount;
    }

    public Long getUvCount() {
        return uvCount;
    }

    public void setUvCount(Long uvCount) {
        this.uvCount = uvCount;
    }

    public Long getUipCount() {
        return uipCount;
    }

    public void setUipCount(Long uipCount) {
        this.uipCount = uipCount;
    }

    public LocalDateTime getLastVisitAt() {
        return lastVisitAt;
    }

    public void setLastVisitAt(LocalDateTime lastVisitAt) {
        this.lastVisitAt = lastVisitAt;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
