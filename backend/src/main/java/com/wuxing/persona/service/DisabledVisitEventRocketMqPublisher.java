package com.wuxing.persona.service;

import com.wuxing.persona.entity.VisitEventEntity;

public class DisabledVisitEventRocketMqPublisher implements VisitEventRocketMqPublisher {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void publish(VisitEventEntity entity) {
        throw new IllegalStateException("RocketMQ visit event publisher is not configured");
    }
}
