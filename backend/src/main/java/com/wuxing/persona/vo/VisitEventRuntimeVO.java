package com.wuxing.persona.vo;

import java.time.LocalDateTime;

public class VisitEventRuntimeVO {

    private int queueSize;
    private int queueCapacity;
    private int drainLimit;
    private long droppedAsyncEvents;
    private long totalFlushedEvents;
    private LocalDateTime lastFlushAt;
    private int lastBatchSize;
    private long batchWriteFailures;
    private boolean workerAlive;
    private String asyncMode;
    private boolean rocketMqAvailable;
    private boolean rocketMqFallbackToLocal;
    private String rocketMqTopic;
    private long rocketMqPublishedEvents;
    private long rocketMqPublishFailures;
    private long rocketMqFallbackEvents;
    private long rocketMqShadowLocalEvents;
    private boolean rocketMqConsumerEnabled;
    private boolean rocketMqConsumerPersistenceReady;
    private String healthStatus;
    private String healthMessage;

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getDrainLimit() {
        return drainLimit;
    }

    public void setDrainLimit(int drainLimit) {
        this.drainLimit = drainLimit;
    }

    public long getDroppedAsyncEvents() {
        return droppedAsyncEvents;
    }

    public void setDroppedAsyncEvents(long droppedAsyncEvents) {
        this.droppedAsyncEvents = droppedAsyncEvents;
    }

    public long getTotalFlushedEvents() {
        return totalFlushedEvents;
    }

    public void setTotalFlushedEvents(long totalFlushedEvents) {
        this.totalFlushedEvents = totalFlushedEvents;
    }

    public LocalDateTime getLastFlushAt() {
        return lastFlushAt;
    }

    public void setLastFlushAt(LocalDateTime lastFlushAt) {
        this.lastFlushAt = lastFlushAt;
    }

    public int getLastBatchSize() {
        return lastBatchSize;
    }

    public void setLastBatchSize(int lastBatchSize) {
        this.lastBatchSize = lastBatchSize;
    }

    public long getBatchWriteFailures() {
        return batchWriteFailures;
    }

    public void setBatchWriteFailures(long batchWriteFailures) {
        this.batchWriteFailures = batchWriteFailures;
    }

    public boolean isWorkerAlive() {
        return workerAlive;
    }

    public void setWorkerAlive(boolean workerAlive) {
        this.workerAlive = workerAlive;
    }

    public String getAsyncMode() {
        return asyncMode;
    }

    public void setAsyncMode(String asyncMode) {
        this.asyncMode = asyncMode;
    }

    public boolean isRocketMqAvailable() {
        return rocketMqAvailable;
    }

    public void setRocketMqAvailable(boolean rocketMqAvailable) {
        this.rocketMqAvailable = rocketMqAvailable;
    }

    public boolean isRocketMqFallbackToLocal() {
        return rocketMqFallbackToLocal;
    }

    public void setRocketMqFallbackToLocal(boolean rocketMqFallbackToLocal) {
        this.rocketMqFallbackToLocal = rocketMqFallbackToLocal;
    }

    public String getRocketMqTopic() {
        return rocketMqTopic;
    }

    public void setRocketMqTopic(String rocketMqTopic) {
        this.rocketMqTopic = rocketMqTopic;
    }

    public long getRocketMqPublishedEvents() {
        return rocketMqPublishedEvents;
    }

    public void setRocketMqPublishedEvents(long rocketMqPublishedEvents) {
        this.rocketMqPublishedEvents = rocketMqPublishedEvents;
    }

    public long getRocketMqPublishFailures() {
        return rocketMqPublishFailures;
    }

    public void setRocketMqPublishFailures(long rocketMqPublishFailures) {
        this.rocketMqPublishFailures = rocketMqPublishFailures;
    }

    public long getRocketMqFallbackEvents() {
        return rocketMqFallbackEvents;
    }

    public void setRocketMqFallbackEvents(long rocketMqFallbackEvents) {
        this.rocketMqFallbackEvents = rocketMqFallbackEvents;
    }

    public long getRocketMqShadowLocalEvents() {
        return rocketMqShadowLocalEvents;
    }

    public void setRocketMqShadowLocalEvents(long rocketMqShadowLocalEvents) {
        this.rocketMqShadowLocalEvents = rocketMqShadowLocalEvents;
    }

    public boolean isRocketMqConsumerEnabled() {
        return rocketMqConsumerEnabled;
    }

    public void setRocketMqConsumerEnabled(boolean rocketMqConsumerEnabled) {
        this.rocketMqConsumerEnabled = rocketMqConsumerEnabled;
    }

    public boolean isRocketMqConsumerPersistenceReady() {
        return rocketMqConsumerPersistenceReady;
    }

    public void setRocketMqConsumerPersistenceReady(boolean rocketMqConsumerPersistenceReady) {
        this.rocketMqConsumerPersistenceReady = rocketMqConsumerPersistenceReady;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthMessage() {
        return healthMessage;
    }

    public void setHealthMessage(String healthMessage) {
        this.healthMessage = healthMessage;
    }
}
