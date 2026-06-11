package com.wuxing.persona.vo;

public class VisitEventRuntimeVO {

    private int queueSize;
    private int queueCapacity;
    private int drainLimit;
    private long droppedAsyncEvents;
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

    public boolean isWorkerAlive() {
        return workerAlive;
    }

    public void setWorkerAlive(boolean workerAlive) {
        this.workerAlive = workerAlive;
    }
}
