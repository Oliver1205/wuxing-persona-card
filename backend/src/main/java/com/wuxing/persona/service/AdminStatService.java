package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.entity.SiteDailyMetricEntity;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.ShortLinkDailyMetricMapper;
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
    private static final int SOURCE_FILTER_SCAN_PAGE_SIZE = 100;
    private static final int SOURCE_FILTER_SCAN_LIMIT = 500;
    private static final int EXPORT_LIMIT = 500;
    private static final int TOP_ATTRIBUTION_LIMIT = 5;
    private static final int TOP_PERSONA_LIMIT = 8;
    private static final int PERSONA_DISTRIBUTION_LIMIT = 120;
    private static final String SYNTHETIC_CHANNEL = "perf-test";

    private final UserResultMapper userResultMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final VisitEventMapper visitEventMapper;
    private final SiteDailyMetricMapper siteDailyMetricMapper;
    private final ShortLinkDailyMetricMapper shortLinkDailyMetricMapper;
    private final ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter;
    private final RedisCacheService redisCacheService;

    public AdminStatService(UserResultMapper userResultMapper,
                            ShortLinkMapper shortLinkMapper,
                            VisitEventMapper visitEventMapper,
                            SiteDailyMetricMapper siteDailyMetricMapper,
                            ShortLinkDailyMetricMapper shortLinkDailyMetricMapper,
                            ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter,
                            RedisCacheService redisCacheService) {
        this.userResultMapper = userResultMapper;
        this.shortLinkMapper = shortLinkMapper;
        this.visitEventMapper = visitEventMapper;
        this.siteDailyMetricMapper = siteDailyMetricMapper;
        this.shortLinkDailyMetricMapper = shortLinkDailyMetricMapper;
        this.externalShortLinkStatsAdapter = externalShortLinkStatsAdapter;
        this.redisCacheService = redisCacheService;
    }

    public AdminOverviewVO overview(AdminDateRange range) {
        return overview(range, false);
    }

    public AdminOverviewVO overview(AdminDateRange range, boolean includeSynthetic) {
        return overview(range, includeSynthetic, false);
    }

    public AdminOverviewVO overview(AdminDateRange range, boolean includeSynthetic, boolean forceRefresh) {
        String excludedChannel = includeSynthetic ? null : SYNTHETIC_CHANNEL;
        String rangeKey = overviewRangeKey(range, excludedChannel);
        if (!forceRefresh) {
            AdminOverviewVO cached = redisCacheService.getAdminOverview(rangeKey);
            if (cached != null) {
                return cached;
            }
        }
        AdminOverviewVO overview = new AdminOverviewVO();
        overview.setSyntheticTrafficExcluded(excludedChannel != null);
        overview.setSyntheticIsolationLevel(excludedChannel == null ? "all_traffic" : "event_channel");
        overview.setSyntheticIsolationNote(excludedChannel == null
                ? "当前包含测试流量，适合复盘压测和巡检，不适合直接判断真实用户增长。"
                : "当前按 visit_event.channel=perf-test 从统计查询中排除测试流量，尚不是 user_result/short_link 实体层强隔离。");
        long startClicks = countEvent(EventType.START_TEST_CLICK, range, excludedChannel);
        long resultCreated = countResults(range, excludedChannel);
        overview.setTotalPv(countEvents(range, excludedChannel));
        overview.setTotalUv(countDistinctClient(range, excludedChannel));
        overview.setTotalUip(countDistinctIp(range, excludedChannel));
        overview.setHomeViews(countEvent(EventType.PAGE_VIEW_HOME, range, excludedChannel));
        overview.setStartClicks(startClicks);
        overview.setTestSubmits(countEvent(EventType.TEST_SUBMIT, range, excludedChannel));
        overview.setResultCreated(resultCreated);
        overview.setShortLinkCreated(countShortLinks(range, excludedChannel));
        overview.setShortLinkVisits(countEvent(EventType.SHORT_LINK_VISIT, range, excludedChannel));
        overview.setCompletionRate(startClicks == 0 ? 0 : Math.round(resultCreated * 10000.0 / startClicks) / 100.0);
        overview.setAverageCompletionSeconds(averageCompletionSeconds(range, excludedChannel));
        DailyTrendResult dailyTrendResult = buildDailyTrends(range, excludedChannel);
        overview.setDailyTrends(dailyTrendResult.records());
        overview.setMetricSource(dailyTrendResult.metricSource());
        overview.setAggregatedThroughDate(dailyTrendResult.aggregatedThroughDate());
        overview.setFunnelSteps(buildFunnelSteps(range, excludedChannel));
        overview.setTopChannels(toNameCounts(listTopChannels(range, excludedChannel)));
        overview.setTopCampaigns(toNameCounts(listTopCampaigns(range, excludedChannel)));
        overview.setPopularElementCombos(toElementCombos(listPopularElementCombos(range, excludedChannel)));
        overview.setPopularStarOfficers(toStarOfficers(listPopularStarOfficers(range, excludedChannel)));
        overview.setPopularPersonas(toNameCounts(listPopularPersonas(range, excludedChannel)));
        overview.setPersonaDistribution(toNameCounts(listPersonaDistribution(range, excludedChannel)));
        overview.setRecentResults(toRecentResults(listRecentResults(range, excludedChannel)));
        overview.setRecentShortLinks(toShortLinkItems(listRecentShortLinks(range, 0, 5, null, excludedChannel),
                range, excludedChannel));
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
        return listShortLinks(page, pageSize, range, keyword, statSource, false);
    }

    public PageVO<ShortLinkListItemVO> listShortLinks(long page,
                                                      long pageSize,
                                                      AdminDateRange range,
                                                      String keyword,
                                                      String statSource,
                                                      boolean includeSynthetic) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSource = normalizeStatSource(statSource);
        String excludedChannel = includeSynthetic ? null : SYNTHETIC_CHANNEL;
        if (normalizedSource != null) {
            return listShortLinksByComputedSource(normalizedPage, normalizedPageSize, range,
                    normalizedKeyword, normalizedSource, excludedChannel);
        }
        long offset = (normalizedPage - 1) * normalizedPageSize;
        return new PageVO<>(
                normalizedPage,
                normalizedPageSize,
                countShortLinksFiltered(range, normalizedKeyword, excludedChannel),
                toShortLinkItems(listRecentShortLinks(range, offset, normalizedPageSize, normalizedKeyword, excludedChannel),
                        range, excludedChannel)
        );
    }

    public AdminShortLinkExportVO exportShortLinks(AdminDateRange range, String keyword, String statSource) {
        return exportShortLinks(range, keyword, statSource, false);
    }

    public AdminShortLinkExportVO exportShortLinks(AdminDateRange range,
                                                   String keyword,
                                                   String statSource,
                                                   boolean includeSynthetic) {
        PageVO<ShortLinkListItemVO> page = listShortLinks(1, EXPORT_LIMIT, range, keyword, statSource,
                includeSynthetic);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("shortCode,resultId,shortUrl,elementCombo,starOfficerName,pv,uv,uip,statSource,metricSource,createdAt,lastVisitAt\n");
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
                    .append(csv(item.getMetricSource())).append(',')
                    .append(csv(item.getCreatedAt() == null ? null : item.getCreatedAt().toString())).append(',')
                    .append(csv(item.getLastVisitAt() == null ? null : item.getLastVisitAt().toString()))
                    .append('\n');
        }
        return new AdminShortLinkExportVO("wuxing-short-links-" + LocalDate.now() + ".csv", csv.toString());
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode, long page, long pageSize, AdminDateRange range) {
        return listShortLinkVisits(shortCode, page, pageSize, range, false);
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode,
                                                        long page,
                                                        long pageSize,
                                                        AdminDateRange range,
                                                        boolean includeSynthetic) {
        return listShortLinkVisits(shortCode, page, pageSize, range, includeSynthetic, null);
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode,
                                                        long page,
                                                        long pageSize,
                                                        AdminDateRange range,
                                                        boolean includeSynthetic,
                                                        String statSource) {
        ShortLinkCodeUtils.validate(shortCode);
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        String excludedChannel = includeSynthetic ? null : SYNTHETIC_CHANNEL;
        String normalizedSource = normalizeStatSource(statSource);
        ShortLinkEntity shortLink = shortLinkMapper.selectByShortCode(shortCode);
        boolean shouldReadExternal = shortLink != null
                && ("external".equals(normalizedSource) || (normalizedSource == null && excludedChannel == null));
        if (shouldReadExternal) {
            java.util.Optional<PageVO<ShortLinkVisitVO>> externalRecords =
                    externalShortLinkStatsAdapter.fetchAccessRecords(shortLink, normalizedPage, normalizedPageSize, range);
            if (externalRecords.isPresent()) {
                return externalRecords.get();
            }
            if ("external".equals(normalizedSource)) {
                return new PageVO<>(normalizedPage, normalizedPageSize, 0, java.util.List.of());
            }
        }
        List<ShortLinkVisitVO> records = listLocalShortLinkVisits(shortCode, range, offset, normalizedPageSize,
                        excludedChannel).stream()
                .map(this::toVisit)
                .toList();
        return new PageVO<>(normalizedPage, normalizedPageSize, countLocalShortLinkVisits(shortCode, range,
                excludedChannel), records);
    }

    private List<VisitEventEntity> listLocalShortLinkVisits(String shortCode,
                                                            AdminDateRange range,
                                                            long offset,
                                                            long pageSize,
                                                            String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.listByShortCodeBetween(shortCode,
                    range.getStartAt(), range.getEndExclusive(), offset, pageSize);
        }
        return visitEventMapper.listByShortCodeBetweenExcludingChannel(shortCode,
                range.getStartAt(), range.getEndExclusive(), offset, pageSize, excludedChannel);
    }

    private long countLocalShortLinkVisits(String shortCode, AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.countByShortCodeBetween(shortCode,
                    range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.countByShortCodeBetweenExcludingChannel(shortCode,
                range.getStartAt(), range.getEndExclusive(), excludedChannel);
    }

    private DailyTrendResult buildDailyTrends(AdminDateRange range, String excludedChannel) {
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
            SiteDailyMetricEntity aggregated = excludedChannel == null && cursor.isBefore(today)
                    ? siteDailyMetricMapper.selectByMetricDate(cursor)
                    : null;
            if (aggregated != null) {
                metric.setDate(aggregated.getMetricDate().toString());
                metric.setPv(aggregated.getPv());
                metric.setResultCreated(aggregated.getResultCreated());
                metric.setShortLinkCreated(aggregated.getShortLinkCreated());
                metric.setShortLinkVisits(aggregated.getShortLinkVisits());
                usedAggregate = true;
                aggregatedThrough = aggregated.getMetricDate();
            } else {
                metric.setDate(cursor.toString());
                AdminDateRange dayRange = AdminDateRange.of(cursor, cursor);
                metric.setPv(countEvents(dayRange, excludedChannel));
                metric.setResultCreated(countResults(dayRange, excludedChannel));
                metric.setShortLinkCreated(countShortLinks(dayRange, excludedChannel));
                metric.setShortLinkVisits(countEvent(EventType.SHORT_LINK_VISIT, dayRange, excludedChannel));
                usedLive = true;
            }
            trends.add(metric);
            cursor = cursor.plusDays(1);
        }
        String source = usedAggregate && usedLive ? "mixed" : usedAggregate ? "daily_metric" : "live_event";
        return new DailyTrendResult(trends, source, aggregatedThrough == null ? null : aggregatedThrough.toString());
    }

    private List<FunnelStepVO> buildFunnelSteps(AdminDateRange range, String excludedChannel) {
        List<FunnelDefinition> definitions = List.of(
                new FunnelDefinition(EventType.PAGE_VIEW_HOME, "首页访问"),
                new FunnelDefinition(EventType.START_TEST_CLICK, "开始测试"),
                new FunnelDefinition(EventType.TEST_FORM_START, "开始填写"),
                new FunnelDefinition(EventType.TEST_SUBMIT_ATTEMPT, "提交尝试"),
                new FunnelDefinition(EventType.TEST_SUBMIT, "提交成功"),
                new FunnelDefinition(EventType.RESULT_VIEW, "查看结果"),
                new FunnelDefinition(EventType.SHARE_PANEL_VIEW, "分享区曝光"),
                new FunnelDefinition(EventType.SHORT_LINK_COPY, "复制短链"),
                new FunnelDefinition(EventType.SAVE_SHARE_IMAGE_SUCCESS, "保存分享图"),
                new FunnelDefinition(EventType.NATIVE_SHARE_SUCCESS, "系统分享"),
                new FunnelDefinition(EventType.SHORT_LINK_VISIT, "短链回流"),
                new FunnelDefinition(EventType.SHARED_RESULT_CTA_CLICK, "回流再测")
        );
        List<FunnelStepVO> steps = new ArrayList<>();
        long previous = -1;
        for (FunnelDefinition definition : definitions) {
            long count = countEvent(definition.eventType(), range, excludedChannel);
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
                                                                       String source,
                                                                       String excludedChannel) {
        long databaseOffset = 0;
        long scanned = 0;
        long filteredOffset = (page - 1) * pageSize;
        long filteredTotal = 0;
        List<ShortLinkListItemVO> pageRecords = new ArrayList<>((int) pageSize);
        while (true) {
            long remainingScanBudget = SOURCE_FILTER_SCAN_LIMIT + 1L - scanned;
            if (remainingScanBudget <= 0) {
                throwSourceFilterScanLimitExceeded();
            }
            long scanPageSize = Math.min(SOURCE_FILTER_SCAN_PAGE_SIZE, remainingScanBudget);
            List<ShortLinkEntity> rows = listRecentShortLinks(range, databaseOffset,
                    scanPageSize, keyword, excludedChannel);
            if (rows.isEmpty()) {
                break;
            }
            scanned += rows.size();
            if (scanned > SOURCE_FILTER_SCAN_LIMIT) {
                throwSourceFilterScanLimitExceeded();
            }
            for (ShortLinkListItemVO item : toShortLinkItems(rows, range, excludedChannel, true)) {
                if (!source.equals(item.getStatSource())) {
                    continue;
                }
                if (filteredTotal >= filteredOffset && pageRecords.size() < pageSize) {
                    pageRecords.add(item);
                }
                filteredTotal++;
            }
            databaseOffset += rows.size();
            if (rows.size() < scanPageSize) {
                break;
            }
        }
        return new PageVO<>(page, pageSize, filteredTotal, pageRecords);
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

    private List<ShortLinkListItemVO> toShortLinkItems(List<ShortLinkEntity> rows,
                                                       AdminDateRange range,
                                                       String excludedChannel) {
        return toShortLinkItems(rows, range, excludedChannel, false);
    }

    private List<ShortLinkListItemVO> toShortLinkItems(List<ShortLinkEntity> rows,
                                                       AdminDateRange range,
                                                       String excludedChannel,
                                                       boolean strictExternalStats) {
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, UserResultEntity> resultsById = userResultMapper.listByResultIds(rows.stream()
                        .map(ShortLinkEntity::getResultId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(UserResultEntity::getResultId, Function.identity()));
        LocalShortLinkStatsResult localStatsResult = loadLocalShortLinkStats(rows, range, excludedChannel);
        Map<String, ShortLinkStats> localStatsByCode = localStatsResult.statsByCode();
        String localMetricSource = localStatsResult.metricSource();
        return rows.stream()
                .map(row -> {
                    UserResultEntity result = resultsById.get(row.getResultId());
                    ShortLinkStats localStats = localStatsByCode.getOrDefault(row.getShortCode(), ShortLinkStats.ZERO);
                    long pv = localStats.pv();
                    long uv = localStats.uv();
                    long uip = localStats.uip();
                    String statSource = "local";
                    String metricSource = localMetricSource;
                    java.util.Optional<ExternalShortLinkStatsSnapshot> externalStats =
                            strictExternalStats
                                    ? externalShortLinkStatsAdapter.fetchStatsStrict(row, range)
                                    : externalShortLinkStatsAdapter.fetchStats(row, range);
                    if (externalStats.isPresent()) {
                        ExternalShortLinkStatsSnapshot snapshot = externalStats.get();
                        pv = snapshot.getPv();
                        uv = snapshot.getUv();
                        uip = snapshot.getUip();
                        statSource = "external";
                        metricSource = "external";
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
                    vo.setMetricSource(metricSource);
                    vo.setLastVisitAt(row.getLastVisitAt());
                    return vo;
                })
                .toList();
    }

    private void throwSourceFilterScanLimitExceeded() {
        throw new BusinessException("statSource filter scans at most " + SOURCE_FILTER_SCAN_LIMIT
                + " short links; narrow date range or keyword before filtering by source");
    }

    private LocalShortLinkStatsResult loadLocalShortLinkStats(List<ShortLinkEntity> rows,
                                                              AdminDateRange range,
                                                              String excludedChannel) {
        List<String> shortCodes = rows.stream()
                .map(ShortLinkEntity::getShortCode)
                .distinct()
                .toList();
        boolean useDailyMetric = excludedChannel == null && hasCompleteDailyMetrics(range);
        List<Map<String, Object>> statRows = useDailyMetric
                ? shortLinkDailyMetricMapper.listStatsBetweenDates(shortCodes, range.getStartDate(), range.getEndDate())
                : listShortLinkStatsBetween(shortCodes, range, excludedChannel);
        String metricSource = useDailyMetric ? "daily_metric" : "live_event";
        Map<String, ShortLinkStats> statsByCode = statRows.stream()
                .collect(Collectors.toMap(row -> value(row, "shortCode", "short_code").toString(), this::toShortLinkStats));
        return new LocalShortLinkStatsResult(statsByCode, metricSource);
    }

    private boolean hasCompleteDailyMetrics(AdminDateRange range) {
        if (range.getStartDate() == null || range.getEndDate() == null || !range.getEndDate().isBefore(LocalDate.now())) {
            return false;
        }
        LocalDate cursor = range.getStartDate();
        while (!cursor.isAfter(range.getEndDate())) {
            if (siteDailyMetricMapper.selectByMetricDate(cursor) == null) {
                return false;
            }
            cursor = cursor.plusDays(1);
        }
        return true;
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

    private long countEvents(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.countAllBetween(range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.countAllBetweenExcludingChannel(range.getStartAt(), range.getEndExclusive(), excludedChannel);
    }

    private long countDistinctClient(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.countDistinctClientBetween(range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.countDistinctClientBetweenExcludingChannel(range.getStartAt(), range.getEndExclusive(),
                excludedChannel);
    }

    private long countDistinctIp(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.countDistinctIpBetween(range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.countDistinctIpBetweenExcludingChannel(range.getStartAt(), range.getEndExclusive(),
                excludedChannel);
    }

    private long countEvent(EventType eventType, AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.countByEventTypeBetween(eventType.name(), range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.countByEventTypeBetweenExcludingChannel(eventType.name(), range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private long countResults(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.countAllBetween(range.getStartAt(), range.getEndExclusive());
        }
        return userResultMapper.countAllBetweenExcludingChannel(range.getStartAt(), range.getEndExclusive(),
                excludedChannel);
    }

    private long countShortLinks(AdminDateRange range, String excludedChannel) {
        return countShortLinksFiltered(range, null, excludedChannel);
    }

    private long countShortLinksFiltered(AdminDateRange range, String keyword, String excludedChannel) {
        if (excludedChannel == null) {
            return shortLinkMapper.countAllBetweenFiltered(range.getStartAt(), range.getEndExclusive(), keyword);
        }
        return shortLinkMapper.countAllBetweenFilteredExcludingChannel(range.getStartAt(), range.getEndExclusive(),
                keyword, excludedChannel);
    }

    private List<Map<String, Object>> listTopChannels(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.listTopChannelsBetween(TOP_ATTRIBUTION_LIMIT, range.getStartAt(),
                    range.getEndExclusive());
        }
        return visitEventMapper.listTopChannelsBetweenExcludingChannel(TOP_ATTRIBUTION_LIMIT, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private List<Map<String, Object>> listTopCampaigns(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.listTopCampaignsBetween(TOP_ATTRIBUTION_LIMIT, range.getStartAt(),
                    range.getEndExclusive());
        }
        return visitEventMapper.listTopCampaignsBetweenExcludingChannel(TOP_ATTRIBUTION_LIMIT, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private List<Map<String, Object>> listPopularElementCombos(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.listPopularElementCombosBetween(5, range.getStartAt(), range.getEndExclusive());
        }
        return userResultMapper.listPopularElementCombosBetweenExcludingChannel(5, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private List<Map<String, Object>> listPopularStarOfficers(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.listPopularStarOfficersBetween(5, range.getStartAt(), range.getEndExclusive());
        }
        return userResultMapper.listPopularStarOfficersBetweenExcludingChannel(5, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private List<Map<String, Object>> listPopularPersonas(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.listPopularPersonasBetween(TOP_PERSONA_LIMIT, range.getStartAt(),
                    range.getEndExclusive());
        }
        return userResultMapper.listPopularPersonasBetweenExcludingChannel(TOP_PERSONA_LIMIT, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private List<Map<String, Object>> listPersonaDistribution(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.listPersonaDistributionBetween(PERSONA_DISTRIBUTION_LIMIT, range.getStartAt(),
                    range.getEndExclusive());
        }
        return userResultMapper.listPersonaDistributionBetweenExcludingChannel(PERSONA_DISTRIBUTION_LIMIT,
                range.getStartAt(), range.getEndExclusive(), excludedChannel);
    }

    private double averageCompletionSeconds(AdminDateRange range, String excludedChannel) {
        double value = visitEventMapper.averageCompletionSecondsBetween(range.getStartAt(), range.getEndExclusive(),
                excludedChannel);
        return Math.round(value * 10.0) / 10.0;
    }

    private List<UserResultEntity> listRecentResults(AdminDateRange range, String excludedChannel) {
        if (excludedChannel == null) {
            return userResultMapper.listRecentBetween(5, range.getStartAt(), range.getEndExclusive());
        }
        return userResultMapper.listRecentBetweenExcludingChannel(5, range.getStartAt(), range.getEndExclusive(),
                excludedChannel);
    }

    private List<ShortLinkEntity> listRecentShortLinks(AdminDateRange range,
                                                       long offset,
                                                       long limit,
                                                       String keyword,
                                                       String excludedChannel) {
        if (excludedChannel == null) {
            return shortLinkMapper.listPageBetweenFiltered(offset, limit, range.getStartAt(), range.getEndExclusive(),
                    keyword);
        }
        return shortLinkMapper.listPageBetweenFilteredExcludingChannel(offset, limit, range.getStartAt(),
                range.getEndExclusive(), keyword, excludedChannel);
    }

    private List<Map<String, Object>> listShortLinkStatsBetween(List<String> shortCodes,
                                                                AdminDateRange range,
                                                                String excludedChannel) {
        if (excludedChannel == null) {
            return visitEventMapper.listShortLinkStatsBetween(shortCodes, range.getStartAt(), range.getEndExclusive());
        }
        return visitEventMapper.listShortLinkStatsBetweenExcludingChannel(shortCodes, range.getStartAt(),
                range.getEndExclusive(), excludedChannel);
    }

    private String overviewRangeKey(AdminDateRange range, String excludedChannel) {
        return String.valueOf(range.getStartAt()) + ':' + range.getEndExclusive() + ":excluded="
                + (excludedChannel == null ? "none" : excludedChannel);
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

    private record LocalShortLinkStatsResult(Map<String, ShortLinkStats> statsByCode, String metricSource) {
    }
}
