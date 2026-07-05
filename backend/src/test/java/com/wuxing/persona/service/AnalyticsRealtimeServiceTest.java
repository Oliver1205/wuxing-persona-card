package com.wuxing.persona.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wuxing.persona.config.AppProperties;
import com.wuxing.persona.dto.AnalyticsSessionRequest;
import com.wuxing.persona.entity.AnalyticsMetricSnapshotEntity;
import com.wuxing.persona.entity.AnalyticsSessionEntity;
import com.wuxing.persona.entity.AnalyticsVisitorEntity;
import com.wuxing.persona.mapper.AnalyticsMetricSnapshotMapper;
import com.wuxing.persona.mapper.AnalyticsSessionMapper;
import com.wuxing.persona.mapper.AnalyticsVisitorMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AnalyticsRealtimeServiceTest {

    @Mock
    private AnalyticsVisitorMapper analyticsVisitorMapper;

    @Mock
    private AnalyticsSessionMapper analyticsSessionMapper;

    @Mock
    private AnalyticsMetricSnapshotMapper metricSnapshotMapper;

    @Mock
    private VisitEventMapper visitEventMapper;

    @Mock
    private VisitEventService visitEventService;

    @Mock
    private HttpServletRequest servletRequest;

    private AnalyticsRealtimeService service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.setHashSalt("test-salt");
        service = new AnalyticsRealtimeService(
                analyticsVisitorMapper,
                analyticsSessionMapper,
                metricSnapshotMapper,
                visitEventMapper,
                visitEventService,
                properties
        );
        lenient().when(servletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void shouldRecoverWhenConcurrentAnalyticsRowsAlreadyExist() {
        when(analyticsVisitorMapper.insert(any(AnalyticsVisitorEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate visitor"));
        when(analyticsSessionMapper.insert(any(AnalyticsSessionEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate session"));
        when(metricSnapshotMapper.insert(any(AnalyticsMetricSnapshotEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate snapshot"));

        AnalyticsSessionRequest request = new AnalyticsSessionRequest();
        request.setVisitorId("visitor-a");
        request.setSessionId("session-a");
        request.setPath("/result/R1");

        service.heartbeat(request, "visitor-a", "session-a", servletRequest);

        verify(analyticsVisitorMapper).updateLastSeen(any(AnalyticsVisitorEntity.class));
        verify(analyticsSessionMapper).updateHeartbeat(any(AnalyticsSessionEntity.class));
        verify(metricSnapshotMapper).update(any(AnalyticsMetricSnapshotEntity.class));
    }

    @Test
    void shouldRetryRealtimeSnapshotWhenCurrentMinuteUpdateDeadlocks() {
        when(metricSnapshotMapper.selectByMetricTime(any())).thenReturn(new AnalyticsMetricSnapshotEntity());
        when(metricSnapshotMapper.update(any(AnalyticsMetricSnapshotEntity.class)))
                .thenThrow(new DeadlockLoserDataAccessException("deadlock", new RuntimeException("lock wait")))
                .thenReturn(1);

        service.realtime();

        verify(metricSnapshotMapper, times(2)).update(any(AnalyticsMetricSnapshotEntity.class));
    }
}
