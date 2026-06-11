package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.entity.SiteDailyMetricEntity;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.SiteDailyMetricMapper;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsAdapter;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsSnapshot;
import com.wuxing.persona.service.shortlink.ShortLinkCodeUtils;
import com.wuxing.persona.vo.AdminOverviewVO;
import com.wuxing.persona.vo.AdminShortLinkExportVO;
import com.wuxing.persona.vo.DailyMetricVO;
import com.wuxing.persona.vo.FunnelStepVO;
import com.wuxing.persona.vo.NameCountVO;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.RecentResultVO;
import com.wuxing.persona.vo.ShortLinkListItemVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminStatService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int MAX_TREND_DAYS = 14;
    private static final int SOURCE_FILTER_SCAN_LIMIT = 500;
    private static final int EXPORT_LIMIT = 500;
    private static final int TOP_ATTRIBUTION_LIMIT = 5;

    private final UserResultMapper userResultMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final VisitEventMapper visitEventMapper;
    private final SiteDailyMetricMapper siteDailyMetricMapper;
    private final ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter;
    private final RedisCacheService redisCacheService;

    public AdminStatService(UserResultMapper userResultMapper,
                            ShortLinkMapper shortLinkMapper,
                            VisitEventMapper visitEventMapper,
                            SiteDailyMetricMapper siteDailyMetricMapper,
                            ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter,
                            RedisCacheService redisCacheService) {
        this.userResultMapper = userResultMapper;
        this.shortLinkMapper = shortLinkMapper;
        this.visitEventMapper = visitEventMapper;
        this.siteDailyMetricMapper = siteDailyMetricMapper;
        this.externalShortLinkStatsAdapter = externalShortLinkStatsAdapter;
        this.redisCacheService = redisCacheService;
    }

    public AdminOverviewVO overview(AdminDateRange range) {
        String rangeKey = overviewRangeKey(range);
        AdminOverviewVO cached = redisCacheService.getAdminOverview(rangeKey);
        if (cached != null) {
            return cached;
        }
        AdminOverviewVO overview = new AdminOverviewVO();
        long startClicks = visitEventMapper.countByEventTypeBetween(EventType.START_TEST_CLICK.name(),
                range.getStartAt(), range.getEndExclusive());
        long resultCreated = userResultMapper.countAllBetween(range.getStartAt(), range.getEndExclusive());
        overview.setTotalPv(visitEventMapper.countAllBetween(range.getStartAt(), range.getEndExclusive()));
        overview.setTotalUv(visitEventMapper.countDistinctClientBetween(range.getStartAt(), range.getEndExclusive()));
        overview.setTotalUip(visitEventMapper.countDistinctIpBetween(range.getStartAt(), range.getEndExclusive()));
        overview.setHomeViews(visitEventMapper.countByEventTypeBetween(EventType.PAGE_VIEW_HOME.name(),
                range.getStartAt(), range.getEndExclusive()));
        overview.setStartClicks(startClicks);
        overview.setTestSubmits(visitEventMapper.countByEventTypeBetween(EventType.TEST_SUBMIT.name(),
                range.getStartAt(), range.getEndExclusive()));
        overview.setResultCreated(resultCreated);
        overview.setShortLinkCreated(shortLinkMapper.countAllBetween(range.getStartAt(), range.getEndExclusive()));
        overview.setShortLinkVisits(visitEventMapper.countByEventTypeBetween(EventType.SHORT_LINK_VISIT.name(),
                range.getStartAt(), range.getEndExclusive()));
        overview.setCompletionRate(startClicks == 0 ? 0 : Math.round(resultCreated * 10000.0 / startClicks) / 100.0);
        DailyTrendResult dailyTrendResult = buildDailyTrends(range);
        overview.setDailyTrends(dailyTrendResult.records());
        overview.setMetricSource(dailyTrendResult.metricSource());
        overview.setAggregatedThroughDate(dailyTrendResult.aggregatedThroughDate());
        overview.setFunnelSteps(buildFunnelSteps(range));
        overview.setTopChannels(toNameCounts(visitEventMapper.listTopChannelsBetween(TOP_ATTRIBUTION_LIMIT,
                range.getStartAt(), range.getEndExclusive())));
        overview.setTopCampaigns(toNameCounts(visitEventMapper.listTopCampaignsBetween(TOP_ATTRIBUTION_LIMIT,
                range.getStartAt(), range.getEndExclusive())));
        overview.setPopularElementCombos(toElementCombos(userResultMapper.listPopularElementCombosBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setPopularStarOfficers(toStarOfficers(userResultMapper.listPopularStarOfficersBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setRecentResults(toRecentResults(userResultMapper.listRecentBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setRecentShortLinks(toShortLinkItems(shortLinkMapper.listPageBetween(0, 5,
                range.getStartAt(), range.getEndExclusive()), range));
        redisCacheService.setAdminOverview(rangeKey, overview);
        return overview;
    }

    public PageVO<ShortLinkListItemVO> listShortLinks(long page, long pageSize, AdminDateRange range) {
        return listShortLinks(page, pageSize, range, null, null);
    }

    public PageVO<ShortLinkListItemVO> listShortLinks(long page,
                                                      long pageSize,
                                                      AdminDateRange range,
                                                      String keyword,
                                                      String statSource) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSource = normalizeStatSource(statSource);
        if (normalizedSource != null) {
            return listShortLinksByComputedSource(normalizedPage, normalizedPageSize, range,
                    normalizedKeyword, normalizedSource);
        }
        long offset = (normalizedPage - 1) * normalizedPageSize;
        return new PageVO<>(
                normalizedPage,
                normalizedPageSize,
                shortLinkMapper.countAllBetweenFiltered(range.getStartAt(), range.getEndExclusive(),
                        normalizedKeyword),
                toShortLinkItems(shortLinkMapper.listPageBetweenFiltered(offset, normalizedPageSize,
                        range.getStartAt(), range.getEndExclusive(), normalizedKeyword), range)
        );
    }

    public AdminShortLinkExportVO exportShortLinks(AdminDateRange range, String keyword, String statSource) {
        PageVO<ShortLinkListItemVO> page = listShortLinks(1, EXPORT_LIMIT, range, keyword, statSource);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,createdAt,lastVisitAt\n");
        for (ShortLinkListItemVO item : page.getRecords()) {
            csv.append(csv(item.getShortCode())).append(',')
                    .append(csv(item.getResultId())).append(',')
                    .append(csv(item.getShortUrl())).append(',')
                    .append(csv(item.getElementCombo())).append(',')
                    .append(csv(item.getStarOfficerName())).append(',')
                    .append(item.getPv()).append(',')
                    .append(item.getUv()).append(',')
                    .append(item.getUip()).append(',')
                    .append(csv(item.getStatSource())).append(',')
                    .append(csv(item.getCreatedAt() == null ? null : item.getCreatedAt().toString())).append(',')
                    .append(csv(item.getLastVisitAt() == null ? null : item.getLastVisitAt().toString()))
                    .append('\n');
        }
        return new AdminShortLinkExportVO("wuxing-short-links-" + LocalDate.now() + ".csv", csv.toString());
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode, long page, long pageSize, AdminDateRange range) {
        ShortLinkCodeUtils.validate(shortCode);
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        ShortLinkEntity shortLink = shortLinkMapper.selectByShortCode(shortCode);
        if (shortLink != null) {
            java.util.Optional<PageVO<ShortLinkVisitVO>> externalRecords =
                    externalShortLinkStatsAdapter.fetchAccessRecords(shortLink, normalizedPage, normalizedPageSize, range);
            if (externalRecords.isPresent()) {
                return externalRecords.get();
            }
        }
        List<ShortLinkVisitVO> records = visitEventMapper.listByShortCodeBetween(shortCode,
                        range.getStartAt(), range.getEndExclusive(), offset, normalizedPageSize).stream()
                .map(this::toVisit)
                .toList();
        return new PageVO<>(normalizedPage, normalizedPageSize, visitEventMapper.countByShortCodeBetween(shortCode,
                range.getStartAt(), range.getEndExclusive()), records);
    }

    private DailyTrendResult buildDailyTrends(AdminDateRange range) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = range.getEndDate() == null ? today : range.getEndDate();
        LocalDate startDate = range.getStartDate() == null
                ? endDate.minusDays(DEFAULT_TREND_DAYS - 1L)
                : range.getStartDate();
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        LocalDate earliestAllowed = endDate.minusDays(MAX_TREND_DAYS - 1L);
        if (startDate.isBefore(earliestAllowed)) {
            startDate = earliestAllowed;
        }

        List<DailyMetricVO> trends = new ArrayList<>();
        boolean usedAggregate = false;
        boolean usedLive = false;
        LocalDate aggregatedThrough = null;
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            DailyMetricVO metric = new DailyMetricVO();
            SiteDailyMetricEntity aggregated = cursor.isBefore(today) ? siteDailyMetricMapper.selectByMetricDate(cursor) : null;
            if (aggregated != null) {
                metric.setDate(aggregated.getMetricDate().toString());
                metric.setPv(aggregated.getPv());
                metric.setResultCreated(aggregated.getResultCreated());
                metric.setShortLinkCreated(aggregated.getShortLinkCreated());
                metric.setShortLinkVisits(aggregated.getShortLinkVisits());
                usedAggregate = true;
                aggregatedThrough = aggregated.getMetricDate();
            } else {
                LocalDateTime dayStart = cursor.atStartOfDay();
                LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
                metric.setDate(cursor.toString());
                metric.setPv(visitEventMapper.countAllBetween(dayStart, dayEnd));
                metric.setResultCreated(userResultMapper.countAllBetween(dayStart, dayEnd));
                metric.setShortLinkCreated(shortLinkMapper.countAllBetween(dayStart, dayEnd));
                metric.setShortLinkVisits(visitEventMapper.countByEventTypeBetween(EventType.SHORT_LINK_VISIT.name(),
                        dayStart, dayEnd));
                usedLive = true;
            }
            trends.add(metric);
            cursor = cursor.plusDays(1);
        }
        String source = usedAggregate && usedLive ? "mixed" : usedAggregate ? "daily_metric" : "live_event";
        return new DailyTrendResult(trends, source, aggregatedThrough == null ? null : aggregatedThrough.toString());
    }

    private List<FunnelStepVO> buildFunnelSteps(AdminDateRange range) {
        List<FunnelDefinition> definitions = List.of(
                new FunnelDefinition(EventType.PAGE_VIEW_HOME, "首页访问"),
                new FunnelDefinition(EventType.START_TEST_CLICK, "开始测试"),
                new FunnelDefinition(EventType.TEST_FORM_START, "开始填写"),
                new FunnelDefinition(EventType.TEST_SUBMIT_ATTEMPT, "提交尝试"),
                new FunnelDefinition(EventType.TEST_SUBMIT, "提交成功"),
                new FunnelDefinition(EventType.RESULT_VIEW, "查看结果"),
                new FunnelDefinition(EventType.SHARE_PANEL_VIEW, "打开分享"),
                new FunnelDefinition(EventType.SHORT_LINK_COPY, "复制短链"),
                new FunnelDefinition(EventType.SHORT_LINK_VISIT, "短链回流"),
                new FunnelDefinition(EventType.SHARED_RESULT_CTA_CLICK, "回流再测")
        );
        List<FunnelStepVO> steps = new ArrayList<>();
        long previous = -1;
        for (FunnelDefinition definition : definitions) {
            long count = visitEventMapper.countByEventTypeBetween(definition.eventType().name(),
                    range.getStartAt(), range.getEndExclusive());
            double conversion = previous < 0 ? 100 : rate(count, previous);
            steps.add(new FunnelStepVO(definition.eventType().name(), definition.label(), count, conversion));
            previous = count;
        }
        return steps;
    }

    private PageVO<ShortLinkListItemVO> listShortLinksByComputedSource(long page,
                                                                       long pageSize,
                                                                       AdminDateRange range,
                                                                       String keyword,
                                                                       String source) {
        List<ShortLinkListItemVO> filtered = toShortLinkItems(shortLinkMapper.listPageBetweenFiltered(0,
                        SOURCE_FILTER_SCAN_LIMIT, range.getStartAt(), range.getEndExclusive(), keyword), range)
                .stream()
                .filter(item -> source.equals(item.getStatSource()))
                .toList();
        long offset = (page - 1) * pageSize;
        List<ShortLinkListItemVO> pageRecords = filtered.stream()
                .skip(offset)
                .limit(pageSize)
                .toList();
        return new PageVO<>(page, pageSize, filtered.size(), pageRecords);
    }

    private List<NameCountVO> toElementCombos(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> {
                    ElementType primary = ElementType.fromCode(value(row, "primaryElement", "primary_element").toString());
                    ElementType secondary = ElementType.fromCode(value(row, "secondaryElement", "secondary_element").toString());
                    return new NameCountVO(primary.getDisplayName() + " / " + secondary.getDisplayName(), toLong(value(row, "count")));
                })
                .toList();
    }

    private List<NameCountVO> toStarOfficers(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new NameCountVO(value(row, "starOfficerName", "star_officer_name").toString(), toLong(value(row, "count"))))
                .toList();
    }

    private List<NameCountVO> toNameCounts(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(row -> new NameCountVO(value(row, "name").toString(), toLong(value(row, "count"))))
                .toList();
    }

    private List<RecentResultVO> toRecentResults(List<UserResultEntity> rows) {
        return rows.stream()
                .map(row -> {
                    RecentResultVO vo = new RecentResultVO();
                    vo.setResultId(row.getResultId());
                    vo.setElementCombo(elementCombo(row.getPrimaryElement(), row.getSecondaryElement()));
                    vo.setStarOfficerName(row.getStarOfficerName());
                    vo.setCreatedAt(row.getCreatedAt());
                    return vo;
                })
                .toList();
    }

    private List<ShortLinkListItemVO> toShortLinkItems(List<ShortLinkEntity> rows, AdminDateRange range) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserResultEntity> resultsById = userResultMapper.listByResultIds(rows.stream()
                        .map(ShortLinkEntity::getResultId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(UserResultEntity::getResultId, Function.identity()));
        Map<String, ShortLinkStats> localStatsByCode = visitEventMapper.listShortLinkStatsBetween(rows.stream()
                        .map(ShortLinkEntity::getShortCode)
                        .distinct()
                        .toList(), range.getStartAt(), range.getEndExclusive())
                .stream()
                .collect(Collectors.toMap(row -> value(row, "shortCode", "short_code").toString(), this::toShortLinkStats));
        return rows.stream()
                .map(row -> {
                    UserResultEntity result = resultsById.get(row.getResultId());
                    ShortLinkStats localStats = localStatsByCode.getOrDefault(row.getShortCode(), ShortLinkStats.ZERO);
                    long pv = localStats.pv();
                    long uv = localStats.uv();
                    long uip = localStats.uip();
                    String statSource = "local";
                    java.util.Optional<ExternalShortLinkStatsSnapshot> externalStats =
                            externalShortLinkStatsAdapter.fetchStats(row, range);
                    if (externalStats.isPresent()) {
                        ExternalShortLinkStatsSnapshot snapshot = externalStats.get();
                        pv = snapshot.getPv();
                        uv = snapshot.getUv();
                        uip = snapshot.getUip();
                        statSource = "external";
                    }
                    ShortLinkListItemVO vo = new ShortLinkListItemVO();
                    vo.setShortCode(row.getShortCode());
                    vo.setShortUrl(row.getShortUrl());
                    vo.setResultId(row.getResultId());
                    vo.setElementCombo(result == null ? "-" : elementCombo(result.getPrimaryElement(), result.getSecondaryElement()));
                    vo.setStarOfficerName(result == null ? "-" : result.getStarOfficerName());
                    vo.setCreatedAt(row.getCreatedAt());
                    vo.setPv(pv);
                    vo.setUv(uv);
                    vo.setUip(uip);
                    vo.setStatSource(statSource);
                    vo.setLastVisitAt(row.getLastVisitAt());
                    return vo;
                })
                .toList();
    }

    private ShortLinkStats toShortLinkStats(Map<String, Object> row) {
        return new ShortLinkStats(toLong(value(row, "pv")), toLong(value(row, "uv")), toLong(value(row, "uip")));
    }

    private ShortLinkVisitVO toVisit(VisitEventEntity entity) {
        ShortLinkVisitVO vo = new ShortLinkVisitVO();
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setEventType(entity.getEventType());
        vo.setClientIdHash(entity.getClientIdHash());
        vo.setIpHash(entity.getIpHash());
        vo.setUserAgentHash(entity.getUserAgentHash());
        vo.setChannel(entity.getChannel());
        vo.setCampaign(entity.getCampaign());
        vo.setDeviceType(entity.getDeviceType());
        vo.setReferer(entity.getReferer());
        vo.setStatSource("local");
        return vo;
    }

    private String elementCombo(String primaryCode, String secondaryCode) {
        return ElementType.fromCode(primaryCode).getDisplayName() + " / " + ElementType.fromCode(secondaryCode).getDisplayName();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private double rate(long count, long base) {
        return base == 0 ? 0 : Math.round(count * 10000.0 / base) / 100.0;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String value = keyword.trim();
        if (value.length() > 64) {
            throw new BusinessException("keyword must be at most 64 characters");
        }
        return value;
    }

    private String normalizeStatSource(String statSource) {
        if (statSource == null || statSource.isBlank()) {
            return null;
        }
        String value = statSource.trim().toLowerCase(java.util.Locale.ROOT);
        if (!"local".equals(value) && !"external".equals(value)) {
            throw new BusinessException("statSource must be local or external");
        }
        return value;
    }

    private String overviewRangeKey(AdminDateRange range) {
        return String.valueOf(range.getStartAt()) + ':' + range.getEndExclusive();
    }

    private String csv(String value) {
        String normalized = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
        if (!normalized.isEmpty() && "=+-@".indexOf(normalized.charAt(0)) >= 0) {
            normalized = "'" + normalized;
        }
        return "\"" + normalized.replace("\"", "\"\"") + "\"";
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

    private record FunnelDefinition(EventType eventType, String label) {
    }

    private record DailyTrendResult(List<DailyMetricVO> records, String metricSource, String aggregatedThroughDate) {
    }

    private record ShortLinkStats(long pv, long uv, long uip) {
        private static final ShortLinkStats ZERO = new ShortLinkStats(0, 0, 0);
    }
}
