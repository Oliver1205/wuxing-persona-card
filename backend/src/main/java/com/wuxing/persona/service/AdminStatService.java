package com.wuxing.persona.service;

import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.entity.VisitEventEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.ShortLinkMapper;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.mapper.VisitEventMapper;
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

    public AdminStatService(UserResultMapper userResultMapper, ShortLinkMapper shortLinkMapper, VisitEventMapper visitEventMapper) {
        this.userResultMapper = userResultMapper;
        this.shortLinkMapper = shortLinkMapper;
        this.visitEventMapper = visitEventMapper;
    }

    public AdminOverviewVO overview() {
        AdminOverviewVO overview = new AdminOverviewVO();
        long startClicks = visitEventMapper.countByEventType(EventType.START_TEST_CLICK.name());
        long resultCreated = userResultMapper.countAll();
        overview.setTotalPv(visitEventMapper.countAll());
        overview.setTotalUv(visitEventMapper.countDistinctClient());
        overview.setTotalUip(visitEventMapper.countDistinctIp());
        overview.setHomeViews(visitEventMapper.countByEventType(EventType.PAGE_VIEW_HOME.name()));
        overview.setStartClicks(startClicks);
        overview.setTestSubmits(visitEventMapper.countByEventType(EventType.TEST_SUBMIT.name()));
        overview.setResultCreated(resultCreated);
        overview.setShortLinkCreated(shortLinkMapper.countAll());
        overview.setShortLinkVisits(visitEventMapper.countByEventType(EventType.SHORT_LINK_VISIT.name()));
        overview.setCompletionRate(startClicks == 0 ? 0 : Math.round(resultCreated * 10000.0 / startClicks) / 100.0);
        overview.setPopularElementCombos(toElementCombos(userResultMapper.listPopularElementCombos(5)));
        overview.setPopularStarOfficers(toStarOfficers(userResultMapper.listPopularStarOfficers(5)));
        overview.setRecentResults(toRecentResults(userResultMapper.listRecent(5)));
        overview.setRecentShortLinks(toShortLinkItems(shortLinkMapper.listPage(0, 5)));
        return overview;
    }

    public PageVO<ShortLinkListItemVO> listShortLinks(long page, long pageSize) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        return new PageVO<>(
                normalizedPage,
                normalizedPageSize,
                shortLinkMapper.countAll(),
                toShortLinkItems(shortLinkMapper.listPage(offset, normalizedPageSize))
        );
    }

    public PageVO<ShortLinkVisitVO> listShortLinkVisits(String shortCode, long page, long pageSize) {
        long normalizedPage = Math.max(1, page);
        long normalizedPageSize = Math.min(100, Math.max(1, pageSize));
        long offset = (normalizedPage - 1) * normalizedPageSize;
        List<ShortLinkVisitVO> records = visitEventMapper.listByShortCode(shortCode, offset, normalizedPageSize).stream()
                .map(this::toVisit)
                .toList();
        return new PageVO<>(normalizedPage, normalizedPageSize, visitEventMapper.countByShortCode(shortCode), records);
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

    private List<ShortLinkListItemVO> toShortLinkItems(List<ShortLinkEntity> rows) {
        return rows.stream()
                .map(row -> {
                    UserResultEntity result = userResultMapper.selectByResultId(row.getResultId());
                    long pv = visitEventMapper.countPvByShortCode(row.getShortCode());
                    long uv = visitEventMapper.countUvByShortCode(row.getShortCode());
                    long uip = visitEventMapper.countUipByShortCode(row.getShortCode());
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
