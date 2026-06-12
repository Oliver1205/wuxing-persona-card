package com.wuxing.persona.service;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.CreateMatchRequest;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.service.shortlink.ShortLinkCodeUtils;
import com.wuxing.persona.vo.MatchCandidateVO;
import com.wuxing.persona.vo.MatchResultVO;
import com.wuxing.persona.vo.ResultDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class MatchService {

    private final ResultService resultService;

    public MatchService(ResultService resultService) {
        this.resultService = resultService;
    }

    public MatchCandidateVO candidate(String shortCode) {
        ResultDetailVO result = resultService.getByShortCodeNoTrack(normalizeShortCode(shortCode));
        MatchCandidateVO candidate = new MatchCandidateVO();
        candidate.setShortCode(result.getShortCode());
        candidate.setResultId(result.getResultId());
        candidate.setDisplayName(displayName(result));
        candidate.setPrimaryElementName(result.getPrimaryElementName());
        candidate.setSecondaryElementName(result.getSecondaryElementName());
        candidate.setKeywords(result.getKeywords());
        candidate.setCreatedAt(result.getCreatedAt());
        return candidate;
    }

    public MatchResultVO create(CreateMatchRequest request, String clientId, HttpServletRequest servletRequest) {
        String partnerShortCode = normalizeShortCode(request.getPartnerShortCode());
        ResultDetailVO partner = resultService.getByShortCodeNoTrack(partnerShortCode);
        ResultDetailVO current = resultService.create(request, clientId, servletRequest);
        return build(partner, current);
    }

    public MatchResultVO get(String partnerShortCode, String currentShortCode) {
        String normalizedPartnerShortCode = normalizeShortCode(partnerShortCode);
        String normalizedCurrentShortCode = normalizeShortCode(currentShortCode);
        if (normalizedPartnerShortCode.equals(normalizedCurrentShortCode)) {
            throw new BusinessException("matching requires two different shortCodes");
        }
        ResultDetailVO partner = resultService.getByShortCodeNoTrack(normalizedPartnerShortCode);
        ResultDetailVO current = resultService.getByShortCodeNoTrack(normalizedCurrentShortCode);
        return build(partner, current);
    }

    private MatchResultVO build(ResultDetailVO partner, ResultDetailVO current) {
        int compatibilityScore = compatibilityScore(partner, current);
        RelationKind relationKind = relationKind(partner, current);

        MatchResultVO result = new MatchResultVO();
        result.setMatchId(partner.getShortCode() + "-" + current.getShortCode());
        result.setPartnerShortCode(partner.getShortCode());
        result.setCurrentShortCode(current.getShortCode());
        result.setPartnerResult(partner);
        result.setCurrentResult(current);
        result.setCompatibilityScore(compatibilityScore);
        result.setRelationLabel(relationKind.label);
        result.setHeadline(displayName(current) + " 与 " + displayName(partner) + " 的相处节奏");
        result.setSummary(summary(partner, current, relationKind));
        result.setStrengths(strengths(partner, current, relationKind));
        result.setSuggestions(suggestions(partner, current));
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    private String normalizeShortCode(String shortCode) {
        String value = shortCode == null ? null : shortCode.trim();
        ShortLinkCodeUtils.validate(value);
        return value;
    }

    private int compatibilityScore(ResultDetailVO partner, ResultDetailVO current) {
        double similarity = distributionSimilarity(partner.getAllElementScores(), current.getAllElementScores());
        int relationBonus = relationBonus(partner, current);
        int score = (int) Math.round(50 + similarity * 0.38 + relationBonus);
        return Math.max(58, Math.min(96, score));
    }

    private double distributionSimilarity(Map<String, Integer> partnerScores, Map<String, Integer> currentScores) {
        double partnerTotal = totalScore(partnerScores);
        double currentTotal = totalScore(currentScores);
        double distance = 0;
        for (ElementType elementType : ElementType.values()) {
            double left = partnerScores.getOrDefault(elementType.name(), 0) * 100.0 / partnerTotal;
            double right = currentScores.getOrDefault(elementType.name(), 0) * 100.0 / currentTotal;
            distance += Math.abs(left - right);
        }
        return Math.max(0, 100 - distance / 2);
    }

    private double totalScore(Map<String, Integer> scores) {
        return Math.max(1, scores.values().stream().mapToInt(Integer::intValue).sum());
    }

    private int relationBonus(ResultDetailVO partner, ResultDetailVO current) {
        ElementType partnerPrimary = ElementType.fromCode(partner.getPrimaryElement());
        ElementType currentPrimary = ElementType.fromCode(current.getPrimaryElement());
        if (partnerPrimary == currentPrimary) {
            return 8;
        }
        if (partner.getSecondaryElement().equals(current.getPrimaryElement())
                || current.getSecondaryElement().equals(partner.getPrimaryElement())) {
            return 6;
        }
        if (generates(partnerPrimary, currentPrimary) || generates(currentPrimary, partnerPrimary)) {
            return 5;
        }
        if (balances(partnerPrimary, currentPrimary) || balances(currentPrimary, partnerPrimary)) {
            return 2;
        }
        return 3;
    }

    private RelationKind relationKind(ResultDetailVO partner, ResultDetailVO current) {
        ElementType partnerPrimary = ElementType.fromCode(partner.getPrimaryElement());
        ElementType currentPrimary = ElementType.fromCode(current.getPrimaryElement());
        if (partnerPrimary == currentPrimary || partner.getSecondaryElement().equals(current.getSecondaryElement())) {
            return RelationKind.RESONANT;
        }
        if (generates(partnerPrimary, currentPrimary) || generates(currentPrimary, partnerPrimary)) {
            return RelationKind.SPARKING;
        }
        if (balances(partnerPrimary, currentPrimary) || balances(currentPrimary, partnerPrimary)) {
            return RelationKind.BOUNDARY;
        }
        return RelationKind.COMPLEMENTARY;
    }

    private boolean generates(ElementType from, ElementType to) {
        return switch (from) {
            case WOOD -> to == ElementType.FIRE;
            case FIRE -> to == ElementType.EARTH;
            case EARTH -> to == ElementType.METAL;
            case METAL -> to == ElementType.WATER;
            case WATER -> to == ElementType.WOOD;
        };
    }

    private boolean balances(ElementType from, ElementType to) {
        return switch (from) {
            case WOOD -> to == ElementType.EARTH;
            case EARTH -> to == ElementType.WATER;
            case WATER -> to == ElementType.FIRE;
            case FIRE -> to == ElementType.METAL;
            case METAL -> to == ElementType.WOOD;
        };
    }

    private String displayName(ResultDetailVO result) {
        return result.getPrimaryElementName() + result.getSecondaryElementName() + "型"
                + firstKeyword(result);
    }

    private String firstKeyword(ResultDetailVO result) {
        if (result.getKeywords() == null || result.getKeywords().isEmpty()) {
            return "探索者";
        }
        return result.getKeywords().get(0);
    }

    private String summary(ResultDetailVO partner, ResultDetailVO current, RelationKind relationKind) {
        return "你偏向用" + current.getPrimaryElementName() + "的方式进入关系，TA 更容易带出"
                + partner.getPrimaryElementName() + "的节奏。整体属于" + relationKind.label
                + "，适合先找到共同目标，再把沟通频率和分工边界说清楚。";
    }

    private List<String> strengths(ResultDetailVO partner, ResultDetailVO current, RelationKind relationKind) {
        List<String> items = new ArrayList<>();
        items.add("你们的主轴组合偏向" + relationKind.label + "，容易在具体事情里看见彼此的价值。");
        items.add(current.getStarOfficerName() + "带来" + firstKeyword(current)
                + "，" + partner.getStarOfficerName() + "补上" + firstKeyword(partner) + "。");
        items.add("当目标明确时，双方的五行分布可以形成一个更完整的观察面。");
        return items;
    }

    private List<String> suggestions(ResultDetailVO partner, ResultDetailVO current) {
        return List.of(
                "先约定沟通节奏，再讨论细节，避免一方觉得太快，另一方觉得太慢。",
                "重要决定可以多留一次复盘，让" + current.getPrimaryElementName() + "的判断和"
                        + partner.getPrimaryElementName() + "的关注点都被看见。",
                "意见不同时先描述事实和感受，再给方案，这样更容易把互补变成协作。"
        );
    }

    private enum RelationKind {
        RESONANT("同频共振型"),
        SPARKING("互相点亮型"),
        BOUNDARY("边界互补型"),
        COMPLEMENTARY("节奏互补型");

        private final String label;

        RelationKind(String label) {
            this.label = label;
        }
    }
}
