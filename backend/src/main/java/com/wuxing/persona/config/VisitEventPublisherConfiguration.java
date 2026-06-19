package com.wuxing.persona.config;

import com.wuxing.persona.service.DisabledVisitEventRocketMqPublisher;
import com.wuxing.persona.service.VisitEventRocketMqPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VisitEventPublisherConfiguration {

    @Bean
    @ConditionalOnMissingBean(VisitEventRocketMqPublisher.class)
    public VisitEventRocketMqPublisher disabledVisitEventRocketMqPublisher() {
        return new DisabledVisitEventRocketMqPublisher();
    }
}
