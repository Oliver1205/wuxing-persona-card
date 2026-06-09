package com.wuxing.persona.service;

import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsAdapter;
import com.wuxing.persona.service.shortlink.ExternalShortLinkStatsSnapshot;
import com.wuxing.persona.vo.AdminOverviewVO;
import com.wuxing.persona.vo.NameCountVO;
import com.wuxing.persona.vo.PageVO;
import com.wuxing.persona.vo.RecentResultVO;
import com.wuxing.persona.vo.ShortLinkListItemVO;
import com.wuxing.persona.vo.ShortLinkVisitVO;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminStatService {

    private final UserResultMapper userResultMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final VisitEventMapper visitEventMapper;
    private final ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter;

    public AdminStatService(UserResultMapper userResultMapper,
                            ShortLinkMapper shortLinkMapper,
                            VisitEventMapper visitEventMapper,
                            ExternalShortLinkStatsAdapter externalShortLinkStatsAdapter) {
        this.userResultMapper = userResultMapper;
        this.shortLinkMapper = shortLinkMapper;
        this.visitEventMapper = visitEventMapper;
        this.externalShortLinkStatsAdapter = externalShortLinkStatsAdapter;
    }

    public AdminOverviewVO overview(AdminDateRange range) {
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
        overview.setPopularElementCombos(toElementCombos(userResultMapper.listPopularElementCombosBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setPopularStarOfficers(toStarOfficers(userResultMapper.listPopularStarOfficersBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setRecentResults(toRecentResults(userResultMapper.listRecentBetween(5,
                range.getStartAt(), range.getEndExclusive())));
        overview.setRecentShortLinks(toShortLinkItems(shortLinkMapper.listPageBetween(0, 5,
                range.getStartAt(), range.getEndExclusive()), range));
        return overview;
    }

    public PageVO<ShortLinkListItemVO> listShortLinks(long page, long pageSize, AdminDateRange range) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        return new PageVO<>(
                normalizedPage,
                normalizedPageSize,
                shortLinkMapper.countAllBetween(range.getStartAt(), range.getEndExclusive()),
                toShortLinkItems(shortLinkMapper.listPageBetween(offset, normalizedPageSize,
                        range.getStartAt(), range.getEndExclusive()), range)
        );
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode, long page, long pageSize, AdminDateRange range) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        List<ShortLinkVisitVO> records = visitEventMapper.listByShortCodeBetween(shortCode,
                        range.getStartAt(), range.getEndExclusive(), offset, normalizedPageSize).stream()
                .map(this::toVisit)
                .toList();
        return new PageVO<>(normalizedPage, normalizedPageSize, visitEventMapper.countByShortCodeBetween(shortCode,
                range.getStartAt(), range.getEndExclusive()), records);
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
        return rows.stream()
                .map(row -> {
                    UserResultEntity result = userResultMapper.selectByResultId(row.getResultId());
                    long pv = visitEventMapper.countPvByShortCodeBetween(row.getShortCode(),
                            range.getStartAt(), range.getEndExclusive());
                    long uv = visitEventMapper.countUvByShortCodeBetween(row.getShortCode(),
                            range.getStartAt(), range.getEndExclusive());
                    long uip = visitEventMapper.countUipByShortCodeBetween(row.getShortCode(),
                            range.getStartAt(), range.getEndExclusive());
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

    private ShortLinkVisitVO toVisit(VisitEventEntity entity) {
        ShortLinkVisitVO vo = new ShortLinkVisitVO();
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setEventType(entity.getEventType());
        vo.setClientIdHash(entity.getClientIdHash());
        vo.setIpHash(entity.getIpHash());
        vo.setUserAgentHash(entity.getUserAgentHash());
        vo.setReferer(entity.getReferer());
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
}
