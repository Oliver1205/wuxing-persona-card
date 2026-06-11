package com.wuxing.persona.service;

import com.wuxing.persona.entity.ShortLinkDailyMetricEntity;
import com.wuxing.persona.entity.SiteDailyMetricEntity;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkDailyMetricMapper;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.SiteDailyMetricMapper;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.vo.AnalyticsAggregationVO;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsAggregationService {

    private static final int MAX_MANUAL_DAYS = 31;

    private final VisitEventMapper visitEventMapper;
    private final UserResultMapper userResultMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final SiteDailyMetricMapper siteDailyMetricMapper;
    private final ShortLinkDailyMetricMapper shortLinkDailyMetricMapper;

    public AnalyticsAggregationService(VisitEventMapper visitEventMapper,
                                       UserResultMapper userResultMapper,
                                       ShortLinkMapper shortLinkMapper,
                                       SiteDailyMetricMapper siteDailyMetricMapper,
                                       ShortLinkDailyMetricMapper shortLinkDailyMetricMapper) {
        this.visitEventMapper = visitEventMapper;
        this.userResultMapper = userResultMapper;
        this.shortLinkMapper = shortLinkMapper;
        this.siteDailyMetricMapper = siteDailyMetricMapper;
        this.shortLinkDailyMetricMapper = shortLinkDailyMetricMapper;
    }

    @Transactional
    public AnalyticsAggregationVO aggregate(AdminDateRange range) {
        LocalDate startDate = range.getStartDate() == null ? LocalDate.now().minusDays(1) : range.getStartDate();
        LocalDate endDate = range.getEndDate() == null ? startDate : range.getEndDate();
        int days = 0;
        long shortLinkRows = 0;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            if (!cursor.isBefore(LocalDate.now())) {
                throw new BusinessException("aggregation can only include closed dates before today");
            }
            if (days >= MAX_MANUAL_DAYS) {
                throw new BusinessException("aggregation range must be at most 31 days");
            }
            shortLinkRows += aggregateOneDay(cursor);
            days += 1;
            cursor = cursor.plusDays(1);
        }

        AnalyticsAggregationVO vo = new AnalyticsAggregationVO();
        vo.setStartDate(startDate.toString());
        vo.setEndDate(endDate.toString());
        vo.setDaysAggregated(days);
        vo.setShortLinkRowsAggregated(shortLinkRows);
        vo.setAggregatedAt(LocalDateTime.now().toString());
        return vo;
    }

    private long aggregateOneDay(LocalDate metricDate) {
        LocalDateTime startAt = metricDate.atStartOfDay();
        LocalDateTime endAt = metricDate.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        SiteDailyMetricEntity site = new SiteDailyMetricEntity();
        site.setMetricDate(metricDate);
        site.setPv(visitEventMapper.countAllBetween(startAt, endAt));
        site.setUv(visitEventMapper.countDistinctClientBetween(startAt, endAt));
        site.setUip(visitEventMapper.countDistinctIpBetween(startAt, endAt));
        site.setHomeViews(visitEventMapper.countByEventTypeBetween(EventType.PAGE_VIEW_HOME.name(), startAt, endAt));
        site.setStartClicks(visitEventMapper.countByEventTypeBetween(EventType.START_TEST_CLICK.name(), startAt, endAt));
        site.setTestSubmits(visitEventMapper.countByEventTypeBetween(EventType.TEST_SUBMIT.name(), startAt, endAt));
        site.setResultCreated(userResultMapper.countAllBetween(startAt, endAt));
        site.setShortLinkCreated(shortLinkMapper.countAllBetween(startAt, endAt));
        site.setShortLinkVisits(visitEventMapper.countByEventTypeBetween(EventType.SHORT_LINK_VISIT.name(), startAt, endAt));
        site.setAggregatedAt(now);

        siteDailyMetricMapper.deleteByMetricDate(metricDate);
        siteDailyMetricMapper.insert(site);

        shortLinkDailyMetricMapper.deleteByMetricDate(metricDate);
        long rows = 0;
        for (Map<String, Object> row : visitEventMapper.listShortLinkDailyMetricsBetween(startAt, endAt)) {
            ShortLinkDailyMetricEntity metric = new ShortLinkDailyMetricEntity();
            metric.setMetricDate(metricDate);
            metric.setShortCode(value(row, "shortCode", "short_code").toString());
            metric.setPv(toLong(value(row, "pv")));
            metric.setUv(toLong(value(row, "uv")));
            metric.setUip(toLong(value(row, "uip")));
            metric.setLastVisitAt(toLocalDateTime(value(row, "lastVisitAt", "last_visit_at")));
            metric.setAggregatedAt(now);
            shortLinkDailyMetricMapper.insert(metric);
            rows += 1;
        }
        return rows;
    }

    private Object value(Map<String, Object> row, String... expectedKeys) {
        for (String expectedKey : expectedKeys) {
            Object directValue = row.get(expectedKey);
            if (directValue != null) {
                return directValue;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(expectedKey)) {
                    return entry.getValue();
                }
            }
        }
        throw new IllegalArgumentException("missing query column: " + String.join("/", expectedKeys));
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }
}
