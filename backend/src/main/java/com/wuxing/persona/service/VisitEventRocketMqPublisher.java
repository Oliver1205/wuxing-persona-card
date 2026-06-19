package com.wuxing.persona.service;

import com.wuxing.persona.entity.VisitEventEntity;

public interface VisitEventRocketMqPublisher {

    boolean isAvailable();

    default boolean isConsumerPersistenceReady() {
        return false;
    }

    void publish(VisitEventEntity entity);
}
