package com.wuxing.persona.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.util.JsonUtils;
import com.wuxing.persona.vo.ResultDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultService {

    private static final Logger log = LoggerFactory.getLogger(ResultService.class);
    private static final DateTimeFormatter RESULT_ID_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int RESULT_ID_RANDOM_BOUND = 1_000_000;
    private static final int MAX_RESULT_ID_RETRY = 5;

    private final UserResultMapper userResultMapper;
    private final ObjectMapper objectMapper;
    private final ElementCalculateService elementCalculateService;
    private final StarOfficerService starOfficerService;
    private final ResultTextService resultTextService;
    private final ShortLinkService shortLinkService;
    private final RedisCacheService redisCacheService;
    private final VisitEventService visitEventService;

    public ResultService(UserResultMapper userResultMapper,
                         ObjectMapper objectMapper,
                         ElementCalculateService elementCalculateService,
                         StarOfficerService starOfficerService,
                         ResultTextService resultTextService,
                         ShortLinkService shortLinkService,
                         RedisCacheService redisCacheService,
                         VisitEventService visitEventService) {
        this.userResultMapper = userResultMapper;
        this.objectMapper = objectMapper;
        this.elementCalculateService = elementCalculateService;
        this.starOfficerService = starOfficerService;
        this.resultTextService = resultTextService;
        this.shortLinkService = shortLinkService;
        this.redisCacheService = redisCacheService;
        this.visitEventService = visitEventService;
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultDetailVO create(CreateResultRequest request, String clientId, HttpServletRequest servletRequest) {
        ElementScoreResult scoreResult = elementCalculateService.calculate(request);
        StarOfficer starOfficer = starOfficerService.byMonth(request.getBirthMonth());
        ResultText resultText = resultTextService.build(scoreResult, starOfficer, request);
        UserResultEntity entity = insertWithResultIdRetry(request, scoreResult, starOfficer, resultText);

        ShortLinkEntity shortLink = shortLinkService.createForResult(entity.getResultId());
        visitEventService.record(EventType.TEST_SUBMIT, "/test", entity.getResultId(), shortLink.getShortCode(), clientId, servletRequest);
        visitEventService.record(EventType.RESULT_CREATED, "/result/" + entity.getResultId(), entity.getResultId(), shortLink.getShortCode(), clientId, servletRequest);
        visitEventService.record(EventType.SHORT_LINK_CREATED, null, entity.getResultId(), shortLink.getShortCode(), clientId, servletRequest);

        ResultDetailVO detail = toDetail(entity, shortLink);
        redisCacheService.setResult(entity.getResultId(), detail);
        return detail;
    }

    private UserResultEntity insertWithResultIdRetry(CreateResultRequest request,
                                                     ElementScoreResult scoreResult,
                                                     StarOfficer starOfficer,
                                                     ResultText resultText) {
        for (int attempt = 1; attempt <= MAX_RESULT_ID_RETRY; attempt++) {
            UserResultEntity entity = buildEntity(request, scoreResult, starOfficer, resultText);
            try {
                userResultMapper.insert(entity);
                return entity;
            } catch (DuplicateKeyException ex) {
                log.warn("Result id collision, retrying, resultId={}, attempt={}", entity.getResultId(), attempt);
            }
        }
        throw new BusinessException("result id generation failed, please retry");
    }

    private UserResultEntity buildEntity(CreateResultRequest request,
                                         ElementScoreResult scoreResult,
                                         StarOfficer starOfficer,
                                         ResultText resultText) {
        LocalDateTime now = LocalDateTime.now();
        UserResultEntity entity = new UserResultEntity();
        entity.setResultId(generateResultId());
        entity.setBirthYear(request.getBirthYear());
        entity.setBirthMonth(request.getBirthMonth());
        entity.setBirthDay(request.getBirthDay());
        entity.setBirthTimeRange(request.getBirthTimeRange());
        entity.setAnswerJson(JsonUtils.toJson(objectMapper, request.getAnswers()));
        entity.setPrimaryElement(scoreResult.getPrimaryElement().name());
        entity.setSecondaryElement(scoreResult.getSecondaryElement().name());
        entity.setPrimaryPercent(scoreResult.getPrimaryPercent());
        entity.setSecondaryPercent(scoreResult.getSecondaryPercent());
        entity.setAllElementScoresJson(JsonUtils.toJson(objectMapper, scoreResult.getAllScores()));
        entity.setPersonaTypeId(resultText.getPersonaTypeId());
        entity.setAccentElement(resultText.getAccentElement());
        entity.setRelationKind(resultText.getRelationKind());
        entity.setPersonaLabel(resultText.getPersonaLabel());
        entity.setDayMasterText(resultText.getDayMasterText());
        entity.setPrimarySecondaryText(resultText.getPrimarySecondaryText());
        entity.setAccentText(resultText.getAccentText());
        entity.setHeavenText(resultText.getHeavenText());
        entity.setHumanText(resultText.getHumanText());
        entity.setStarOfficerText(resultText.getStarOfficerText());
        entity.setGrowthAdviceJson(JsonUtils.toJson(objectMapper,
                resultText.getGrowthAdvice() == null ? List.of() : resultText.getGrowthAdvice()));
        entity.setStarOfficerCode(starOfficer.getCode());
        entity.setStarOfficerName(starOfficer.getName());
        entity.setKeywordsJson(JsonUtils.toJson(objectMapper, resultText.getKeywords()));
        entity.setLayoutExplanation(resultText.getLayoutExplanation());
        entity.setStrengthText(resultText.getStrengthText());
        entity.setRelationshipText(resultText.getRelationshipText());
        entity.setCardImageKey(scoreResult.getPrimaryElement().name().toLowerCase() + "_default");
        entity.setStatus(1);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    public ResultDetailVO getByResultId(String resultId, String clientId, HttpServletRequest request) {
        ResultDetailVO cached = redisCacheService.getResult(resultId);
        if (cached != null) {
            visitEventService.recordAsync(EventType.RESULT_VIEW, "/result/" + resultId, resultId, cached.getShortCode(), clientId, request);
            return cached;
        }
        UserResultEntity entity = userResultMapper.selectByResultId(resultId);
        if (entity == null) {
            throw new BusinessException(404, "result not found");
        }
        ShortLinkEntity shortLink = shortLinkService.getByResultId(resultId);
        ResultDetailVO detail = toDetail(entity, shortLink);
        redisCacheService.setResult(resultId, detail);
        visitEventService.recordAsync(EventType.RESULT_VIEW, "/result/" + resultId, resultId, detail.getShortCode(), clientId, request);
        return detail;
    }

    public ResultDetailVO getByResultIdNoTrack(String resultId) {
        UserResultEntity entity = userResultMapper.selectByResultId(resultId);
        if (entity == null) {
            throw new BusinessException(404, "result not found");
        }
        return toDetail(entity, shortLinkService.getByResultId(resultId));
    }

    public ResultDetailVO getByShortCodeNoTrack(String shortCode) {
        ShortLinkEntity shortLink = shortLinkService.getByShortCode(shortCode);
        if (shortLink == null) {
            throw new BusinessException(404, "shortCode not found");
        }
        UserResultEntity entity = userResultMapper.selectByResultId(shortLink.getResultId());
        if (entity == null) {
            throw new BusinessException(404, "result not found");
        }
        return toDetail(entity, shortLink);
    }

    private ResultDetailVO toDetail(UserResultEntity entity, ShortLinkEntity shortLink) {
        ElementType primary = ElementType.fromCode(entity.getPrimaryElement());
        ElementType secondary = ElementType.fromCode(entity.getSecondaryElement());
        Map<String, Integer> scores = JsonUtils.fromJson(objectMapper, entity.getAllElementScoresJson(), new TypeReference<>() { });
        List<String> keywords = JsonUtils.fromJson(objectMapper, entity.getKeywordsJson(), new TypeReference<>() { });
        List<GrowthAdvice> growthAdvice = parseGrowthAdvice(entity.getGrowthAdviceJson());
        ElementType accent = parseElementOrNull(entity.getAccentElement());
        String personaTypeId = coalesce(entity.getPersonaTypeId(), primary.name() + "-" + secondary.name());
        RelationKind relationKind = parseRelationKind(entity.getRelationKind());
        StarTone starTone = resolveStarTone(personaTypeId, primary, secondary, accent, relationKind);
        ResultDetailVO detail = new ResultDetailVO();
        detail.setResultId(entity.getResultId());
        detail.setPrimaryElement(primary.name());
        detail.setPrimaryElementName(primary.getDisplayName());
        detail.setPrimaryPercent(entity.getPrimaryPercent());
        detail.setSecondaryElement(secondary.name());
        detail.setSecondaryElementName(secondary.getDisplayName());
        detail.setSecondaryPercent(entity.getSecondaryPercent());
        detail.setAllElementScores(scores);
        detail.setPersonaTypeId(personaTypeId);
        detail.setAccentElement(accent != null ? accent.name() : "");
        detail.setAccentElementName(accent != null ? accent.getDisplayName() : "");
        detail.setRelationKind(coalesce(entity.getRelationKind(), ""));
        detail.setPersonaLabel(normalizePersonaLabel(entity.getPersonaLabel(), starTone));
        detail.setStarToneName(starTone.getStarToneName());
        detail.setStarToneLabel(starTone.getStarToneLabel());
        detail.setStructureTitle(starTone.getStructureTitle());
        detail.setHeroSummary(starTone.getHeroSummary());
        detail.setIdentityLine(starTone.getIdentityLine());
        detail.setStarToneExplanation(starTone.getStarToneExplanation());
        detail.setDayMasterText(coalesce(entity.getDayMasterText(), entity.getLayoutExplanation()));
        detail.setPrimarySecondaryText(coalesce(entity.getPrimarySecondaryText(), entity.getStrengthText()));
        detail.setAccentText(coalesce(entity.getAccentText(), ""));
        detail.setHeavenText(coalesce(entity.getHeavenText(), ""));
        detail.setHumanText(coalesce(entity.getHumanText(), entity.getRelationshipText()));
        detail.setStarOfficerText(normalizeStarOfficerText(coalesce(entity.getStarOfficerText(),
                "你的出生月份对应「" + entity.getStarOfficerName()
                        + "」，这里把它作为传统星宿体系里的记忆名称，帮助这张人格卡更容易被记住。"),
                entity.getStarOfficerName()));
        detail.setGrowthAdvice(growthAdvice);
        detail.setStarOfficerCode(entity.getStarOfficerCode());
        detail.setStarOfficerName(entity.getStarOfficerName());
        detail.setKeywords(keywords == null || keywords.isEmpty() ? starTone.getKeywords() : keywords);
        detail.setLayoutExplanation(entity.getLayoutExplanation());
        detail.setStrengthText(entity.getStrengthText());
        detail.setRelationshipText(entity.getRelationshipText());
        detail.setCardImageKey(entity.getCardImageKey());
        if (shortLink != null) {
            detail.setShortCode(shortLink.getShortCode());
            detail.setShortUrl(shortLink.getShortUrl());
        }
        detail.setCreatedAt(entity.getCreatedAt());
        return detail;
    }

    private ElementType parseElementOrNull(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return ElementType.fromCode(code);
    }

    private RelationKind parseRelationKind(String code) {
        if (RelationKind.BALANCED.getCode().equals(code)) {
            return RelationKind.BALANCED;
        }
        return RelationKind.DOMINANT;
    }

    private StarTone resolveStarTone(String personaTypeId,
                                     ElementType primary,
                                     ElementType secondary,
                                     ElementType accent,
                                     RelationKind relationKind) {
        try {
            return StarToneRegistry.get(personaTypeId);
        } catch (RuntimeException ignored) {
            ElementType safeAccent = accent == null ? fallbackAccent(primary, secondary) : accent;
            return StarToneRegistry.get(primary, secondary, safeAccent, relationKind);
        }
    }

    private ElementType fallbackAccent(ElementType primary, ElementType secondary) {
        for (ElementType elementType : ElementType.values()) {
            if (elementType != primary && elementType != secondary) {
                return elementType;
            }
        }
        return ElementType.FIRE;
    }

    private List<GrowthAdvice> parseGrowthAdvice(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return JsonUtils.fromJson(objectMapper, json, new TypeReference<>() { });
    }

    private String coalesce(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizePersonaLabel(String value, StarTone starTone) {
        if (isValidPersonaLabel(value)) {
            return value;
        }
        return starTone.getStarToneName();
    }

    private boolean isValidPersonaLabel(String value) {
        return PersonaNameRegistry.isValidPersonaName(value);
    }

    private String normalizeStarOfficerText(String value, String starOfficerName) {
        if (value == null || value.isBlank()) {
            return "你的出生月份对应「" + starOfficerName + "」，这里把它作为传统星宿体系里的记忆名称。";
        }
        if (value.startsWith("星宿锚点：")) {
            String name = value.substring("星宿锚点：".length()).trim();
            String displayName = name.isBlank() ? starOfficerName : name;
            return "你的出生月份对应「" + displayName + "」，这里把它作为传统星宿体系里的记忆名称，帮助这张人格卡更容易被记住。";
        }
        return value;
    }

    private String generateResultId() {
        int suffix = ThreadLocalRandom.current().nextInt(RESULT_ID_RANDOM_BOUND);
        return "R" + LocalDateTime.now().format(RESULT_ID_TIME_FORMAT) + String.format("%06d", suffix);
    }
}
