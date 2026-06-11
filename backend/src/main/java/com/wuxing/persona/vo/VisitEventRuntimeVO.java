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
}
