package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.vo.VisitEventRuntimeVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VisitEventServiceTest {

    @Mock
    private VisitEventMapper visitEventMapper;

    @Mock
    private HttpServletRequest request;

    private VisitEventService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setHashSalt("test-salt");
        service = localService(appProperties);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void recordShouldStripQueryAndFragmentFromAbsoluteReferer() {
        stubRequest("https://example.com/result/R1?sc=abc123&token=secret#share");

        service.record(EventType.RESULT_VIEW, "/result/R1", "R1", "abc123", "client-a", request);

        VisitEventEntity entity = capturedEntity();
        assertEquals("https://example.com/result/R1", entity.getReferer());
    }

    @Test
    void recordShouldStripQueryAndFragmentFromRelativeReferer() {
        stubRequest("/result/R2?sc=abc123#card");

        service.record(EventType.RESULT_VIEW, "/result/R2", "R2", "abc123", "client-a", request);

        VisitEventEntity entity = capturedEntity();
        assertEquals("/result/R2", entity.getReferer());
    }

    @Test
    void recordShouldNotBreakMainFlowWhenInsertFails() {
        stubRequest("/s/abc123?channel=share");
        doThrow(new RuntimeException("database busy")).when(visitEventMapper).insert(any());

        assertDoesNotThrow(() ->
                service.record(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request));
    }

    @Test
    void recordAsyncShouldInsertOutsideCallerThread() {
        stubRequest("/s/abc123?channel=share");

        service.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

        ArgumentCaptor<List<VisitEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(visitEventMapper, timeout(1000)).insertBatch(captor.capture());
        assertEquals("SHORT_LINK_VISIT", captor.getValue().get(0).getEventType());
        assertEquals("abc123", captor.getValue().get(0).getShortCode());
        assertEquals(1, service.runtime().getTotalFlushedEvents());
        assertEquals(1, service.runtime().getLastBatchSize());
        assertNotNull(service.runtime().getLastFlushAt());
        assertEquals("ok", service.runtime().getHealthStatus());
    }

    @Test
    void recordAsyncShouldInsertInCallerThreadWhenSyncModeEnabled() {
        stubRequest("/s/abc123?channel=share");
        AppProperties appProperties = new AppProperties();
        appProperties.setHashSalt("test-salt");
        AppProperties.VisitEventProperties visitEventProperties = new AppProperties.VisitEventProperties();
        visitEventProperties.setAsyncMode("sync");
        appProperties.setVisitEvent(visitEventProperties);
        VisitEventService customService = localService(appProperties);

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

            ArgumentCaptor<VisitEventEntity> captor = ArgumentCaptor.forClass(VisitEventEntity.class);
            verify(visitEventMapper).insert(captor.capture());
            assertEquals("SHORT_LINK_VISIT", captor.getValue().getEventType());
            assertEquals("abc123", captor.getValue().getShortCode());
            assertEquals("sync", customService.runtime().getAsyncMode());
            assertEquals(1, customService.runtime().getTotalFlushedEvents());
            assertEquals("ok", customService.runtime().getHealthStatus());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void recordAsyncShouldPublishToRocketMqWhenModeEnabledAndPublisherAvailable() {
        stubRequest("/s/abc123?channel=share");
        AppProperties appProperties = rocketMqProperties(true);
        CapturingRocketMqPublisher publisher = new CapturingRocketMqPublisher(true);
        VisitEventService customService = new VisitEventService(visitEventMapper, appProperties, publisher);

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

            VisitEventRuntimeVO runtime = customService.runtime();
            assertEquals("rocketmq", runtime.getAsyncMode());
            assertTrue(runtime.isRocketMqAvailable());
            assertEquals(1, runtime.getRocketMqPublishedEvents());
            assertEquals(0, runtime.getRocketMqPublishFailures());
            assertEquals(1, runtime.getRocketMqShadowLocalEvents());
            assertEquals("watch", runtime.getHealthStatus());
            assertEquals("abc123", publisher.lastEvent().getShortCode());
            assertEquals("wuxing-test-event", runtime.getRocketMqTopic());
            ArgumentCaptor<List<VisitEventEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(visitEventMapper, timeout(1000)).insertBatch(captor.capture());
            assertEquals("abc123", captor.getValue().get(0).getShortCode());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void recordAsyncShouldKeepShadowLocalQueueWhenConsumerSwitchIsEnabledButPersistenceIsNotReady() {
        stubRequest("/s/abc123?channel=share");
        AppProperties appProperties = rocketMqProperties(true);
        appProperties.getVisitEvent().getRocketmq().setConsumerEnabled(true);
        CapturingRocketMqPublisher publisher = new CapturingRocketMqPublisher(true);
        VisitEventService customService = new VisitEventService(visitEventMapper, appProperties, publisher);

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

            ArgumentCaptor<List<VisitEventEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(visitEventMapper, timeout(1000)).insertBatch(captor.capture());
            VisitEventRuntimeVO runtime = customService.runtime();
            assertEquals(1, runtime.getRocketMqPublishedEvents());
            assertEquals(1, runtime.getRocketMqShadowLocalEvents());
            assertTrue(runtime.isRocketMqConsumerEnabled());
            assertEquals(false, runtime.isRocketMqConsumerPersistenceReady());
            assertEquals("watch", runtime.getHealthStatus());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void recordAsyncShouldFallbackToLocalQueueWhenRocketMqUnavailable() {
        stubRequest("/s/abc123?channel=share");
        AppProperties appProperties = rocketMqProperties(true);
        CapturingRocketMqPublisher publisher = new CapturingRocketMqPublisher(false);
        VisitEventService customService = new VisitEventService(visitEventMapper, appProperties, publisher);

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

            ArgumentCaptor<List<VisitEventEntity>> captor = ArgumentCaptor.forClass(List.class);
            verify(visitEventMapper, timeout(1000)).insertBatch(captor.capture());
            assertEquals("SHORT_LINK_VISIT", captor.getValue().get(0).getEventType());
            assertEquals(1, customService.runtime().getRocketMqPublishFailures());
            assertEquals(1, customService.runtime().getRocketMqFallbackEvents());
            assertEquals(1, customService.runtime().getTotalFlushedEvents());
            assertEquals("watch", customService.runtime().getHealthStatus());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void recordAsyncShouldDropWhenRocketMqUnavailableAndFallbackDisabled() {
        stubRequest("/s/abc123?channel=share");
        AppProperties appProperties = rocketMqProperties(false);
        VisitEventService customService = new VisitEventService(
                visitEventMapper, appProperties, new CapturingRocketMqPublisher(false));

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

            VisitEventRuntimeVO runtime = customService.runtime();
            assertEquals(1, runtime.getRocketMqPublishFailures());
            assertEquals(1, runtime.getDroppedAsyncEvents());
            assertEquals(0, runtime.getTotalFlushedEvents());
            assertEquals("danger", runtime.getHealthStatus());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void recordAsyncShouldKeepAttributionFields() {
        stubRequest("/?channel=organic");

        service.recordAsync(EventType.PAGE_VIEW_HOME, "/", null, null, "client-a", request,
                "session-a", "Organic Search", "Spring Launch");

        ArgumentCaptor<List<VisitEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(visitEventMapper, timeout(1000)).insertBatch(captor.capture());
        assertEquals("organic-search", captor.getValue().get(0).getChannel());
        assertEquals("spring-launch", captor.getValue().get(0).getCampaign());
    }

    @Test
    void recordAsyncShouldFallbackToSingleInsertWhenBatchFails() {
        stubRequest("/s/abc123?channel=share");
        doThrow(new RuntimeException("batch busy")).when(visitEventMapper).insertBatch(any());

        service.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

        verify(visitEventMapper, timeout(1000).atLeastOnce()).insertBatch(any());
        verify(visitEventMapper, timeout(1000).atLeastOnce()).insert(any(VisitEventEntity.class));
        assertEquals(1, service.runtime().getBatchWriteFailures());
        assertEquals(1, service.runtime().getTotalFlushedEvents());
        assertEquals("danger", service.runtime().getHealthStatus());
    }

    @Test
    void recordAsyncShouldNotCountFlushWhenBatchAndFallbackInsertFail() {
        stubRequest("/s/abc123?channel=share");
        doThrow(new RuntimeException("batch busy")).when(visitEventMapper).insertBatch(any());
        doThrow(new RuntimeException("single busy")).when(visitEventMapper).insert(any(VisitEventEntity.class));

        service.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);

        verify(visitEventMapper, timeout(1000).atLeastOnce()).insertBatch(any());
        verify(visitEventMapper, timeout(1000).atLeastOnce()).insert(any(VisitEventEntity.class));
        assertEquals(1, service.runtime().getBatchWriteFailures());
        assertEquals(0, service.runtime().getTotalFlushedEvents());
        assertEquals("danger", service.runtime().getHealthStatus());
    }

    @Test
    void runtimeShouldExposeConfiguredAsyncPressureKnobs() {
        AppProperties appProperties = new AppProperties();
        AppProperties.VisitEventProperties visitEventProperties = new AppProperties.VisitEventProperties();
        visitEventProperties.setAsyncQueueCapacity(7);
        visitEventProperties.setAsyncDrainLimit(3);
        appProperties.setVisitEvent(visitEventProperties);

        VisitEventService customService = localService(appProperties);

        try {
            assertEquals(7, customService.runtime().getQueueCapacity());
            assertEquals(3, customService.runtime().getDrainLimit());
            assertEquals("ok", customService.runtime().getHealthStatus());
        } finally {
            customService.shutdown();
        }
    }

    @Test
    void visitEventAsyncModeShouldRejectUnknownValue() {
        AppProperties.VisitEventProperties visitEventProperties = new AppProperties.VisitEventProperties();

        assertThrows(IllegalArgumentException.class, () -> visitEventProperties.setAsyncMode("rocket-mq"));
    }

    @Test
    void recordAsyncShouldDropWhenQueueIsFullAndExposeRuntimeCounter() throws InterruptedException {
        AppProperties appProperties = new AppProperties();
        AppProperties.VisitEventProperties visitEventProperties = new AppProperties.VisitEventProperties();
        visitEventProperties.setAsyncQueueCapacity(1);
        visitEventProperties.setAsyncDrainLimit(1);
        appProperties.setVisitEvent(visitEventProperties);
        CountDownLatch firstBatchStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        when(visitEventMapper.insertBatch(any())).thenAnswer(invocation -> {
            firstBatchStarted.countDown();
            releaseWorker.await(2, TimeUnit.SECONDS);
            return 1;
        });
        stubRequest("/s/abc123");
        VisitEventService customService = localService(appProperties);

        try {
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-a", request);
            assertTrue(firstBatchStarted.await(1, TimeUnit.SECONDS));
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-b", request);
            customService.recordAsync(EventType.SHORT_LINK_VISIT, "/s/abc123", "R1", "abc123", "client-c", request);

            assertEquals(1, customService.runtime().getQueueCapacity());
            assertEquals(1, customService.runtime().getQueueSize());
            assertEquals(1, customService.runtime().getDroppedAsyncEvents());
            assertTrue(customService.runtime().isWorkerAlive());
            assertEquals("danger", customService.runtime().getHealthStatus());
        } finally {
            releaseWorker.countDown();
            customService.shutdown();
        }
    }

    @Test
    void recordShouldTrimEventDimensionsToDatabaseBounds() {
        stubRequest("/result/R1");

        service.record(EventType.SHARE_PANEL_VIEW, " /result/" + "x".repeat(300),
                "R" + "1".repeat(100), "a".repeat(80), "client-a", request);

        VisitEventEntity entity = capturedEntity();
        assertEquals(255, entity.getPagePath().length());
        assertEquals(64, entity.getResultId().length());
        assertEquals(32, entity.getShortCode().length());
    }

    private void stubRequest(String referer) {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader(anyString())).thenAnswer(invocation -> switch (invocation.getArgument(0, String.class)) {
            case "Referer" -> referer;
            case "User-Agent" -> "JUnit";
            default -> null;
        });
    }

    private AppProperties rocketMqProperties(boolean fallbackToLocal) {
        AppProperties appProperties = new AppProperties();
        appProperties.setHashSalt("test-salt");
        AppProperties.VisitEventProperties visitEventProperties = new AppProperties.VisitEventProperties();
        visitEventProperties.setAsyncMode("rocketmq");
        visitEventProperties.getRocketmq().setTopic("wuxing-test-event");
        visitEventProperties.getRocketmq().setFallbackToLocal(fallbackToLocal);
        appProperties.setVisitEvent(visitEventProperties);
        return appProperties;
    }

    private VisitEventService localService(AppProperties appProperties) {
        return new VisitEventService(visitEventMapper, appProperties, new DisabledVisitEventRocketMqPublisher());
    }

    private VisitEventEntity capturedEntity() {
        ArgumentCaptor<VisitEventEntity> captor = ArgumentCaptor.forClass(VisitEventEntity.class);
        verify(visitEventMapper).insert(captor.capture());
        return captor.getValue();
    }

    private static class CapturingRocketMqPublisher implements VisitEventRocketMqPublisher {

        private final boolean available;
        private final AtomicReference<VisitEventEntity> lastEvent = new AtomicReference<>();

        private CapturingRocketMqPublisher(boolean available) {
            this.available = available;
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public void publish(VisitEventEntity entity) {
            if (!available) {
                throw new IllegalStateException("publisher unavailable");
            }
            lastEvent.set(entity);
        }

        private VisitEventEntity lastEvent() {
            return lastEvent.get();
        }
    }
}
