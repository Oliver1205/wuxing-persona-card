package com.wuxing.persona.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuxing.persona.dto.AnswerRequest;
import com.wuxing.persona.dto.CreateResultRequest;
import com.wuxing.persona.entity.ShortLinkEntity;
import com.wuxing.persona.entity.UserResultEntity;
import com.wuxing.persona.enums.ElementType;
import com.wuxing.persona.enums.EventType;
import com.wuxing.persona.mapper.UserResultMapper;
import com.wuxing.persona.vo.ResultDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private UserResultMapper userResultMapper;

    @Mock
    private ElementCalculateService elementCalculateService;

    @Mock
    private StarOfficerService starOfficerService;

    @Mock
    private ResultTextService resultTextService;

    @Mock
    private ShortLinkService shortLinkService;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private VisitEventService visitEventService;

    @Mock
    private HttpServletRequest servletRequest;

    private ResultService resultService;

    @BeforeEach
    void setUp() {
        resultService = new ResultService(
                userResultMapper,
                new ObjectMapper(),
                elementCalculateService,
                starOfficerService,
                resultTextService,
                shortLinkService,
                redisCacheService,
                visitEventService);
    }

    @Test
    void createShouldRetryWhenGeneratedResultIdCollides() {
        CreateResultRequest request = buildRequest();
        ElementScoreResult scoreResult = buildScoreResult();
        StarOfficer starOfficer = new StarOfficer("zheng-guan", "正官", ElementType.WOOD, List.of("秩序"));
        ResultText resultText = buildResultText();
        ShortLinkEntity shortLink = buildShortLink();

        when(elementCalculateService.calculate(request)).thenReturn(scoreResult);
        when(starOfficerService.byMonth(6)).thenReturn(starOfficer);
        when(resultTextService.build(scoreResult, starOfficer, request)).thenReturn(resultText);
        when(userResultMapper.insert(any(UserResultEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate result_id"))
                .thenReturn(1);
        when(shortLinkService.createForResult(any())).thenReturn(shortLink);

        ResultDetailVO detail = resultService.create(request, "client-a", servletRequest);

        ArgumentCaptor<UserResultEntity> entityCaptor = ArgumentCaptor.forClass(UserResultEntity.class);
        verify(userResultMapper, times(2)).insert(entityCaptor.capture());
        List<UserResultEntity> insertedEntities = entityCaptor.getAllValues();
        String retryResultId = insertedEntities.get(1).getResultId();

        assertTrue(retryResultId.matches("R\\d{23}"));
        assertEquals(retryResultId, detail.getResultId());
        assertEquals("WOOD-FIRE-EARTH-dominant", insertedEntities.get(1).getPersonaTypeId());
        assertEquals("贪狼成垣", detail.getPersonaLabel());
        assertEquals("贪狼成垣", detail.getStarToneName());
        verify(shortLinkService).createForResult(retryResultId);
        verify(redisCacheService).setResult(eq(retryResultId), any(ResultDetailVO.class));
        verify(visitEventService).record(EventType.TEST_SUBMIT, "/test", retryResultId, "abc123", "client-a", servletRequest);
        verify(visitEventService).record(EventType.RESULT_CREATED, "/result/" + retryResultId,
                retryResultId, "abc123", "client-a", servletRequest);
        verify(visitEventService).record(EventType.SHORT_LINK_CREATED, null,
                retryResultId, "abc123", "client-a", servletRequest);
    }

    @Test
    void getByResultIdShouldNormalizeLegacyPersonaLabel() {
        UserResultEntity entity = buildStoredEntity();
        entity.setPersonaLabel("流动的砾坡");
        when(userResultMapper.selectByResultId("R-old")).thenReturn(entity);
        when(shortLinkService.getByResultId("R-old")).thenReturn(null);

        ResultDetailVO detail = resultService.getByResultIdNoTrack("R-old");

        assertEquals("太阴化衡", detail.getPersonaLabel());
        assertEquals("太阴化衡", detail.getStarToneName());
        assertEquals(4, detail.getPersonaLabel().codePointCount(0, detail.getPersonaLabel().length()));
    }

    @Test
    void getByResultIdShouldRejectMalformedPersonaLabelShape() {
        when(userResultMapper.selectByResultId("R-bad-start")).thenReturn(storedEntityWithLabel("R-bad-start", "的酷沙砾"));
        when(userResultMapper.selectByResultId("R-bad-repeat")).thenReturn(storedEntityWithLabel("R-bad-repeat", "酷的沙的"));
        when(shortLinkService.getByResultId(any())).thenReturn(null);

        ResultDetailVO badStart = resultService.getByResultIdNoTrack("R-bad-start");
        ResultDetailVO badRepeat = resultService.getByResultIdNoTrack("R-bad-repeat");

        assertEquals("太阴化衡", badStart.getPersonaLabel());
        assertEquals("太阴化衡", badRepeat.getPersonaLabel());
    }

    private CreateResultRequest buildRequest() {
        CreateResultRequest request = new CreateResultRequest();
        request.setBirthYear(2000);
        request.setBirthMonth(6);
        request.setBirthDay(18);
        request.setBirthTimeRange("09:00-11:00");
        request.setAnswers(List.of(
                answer("q1", "a"),
                answer("q2", "b"),
                answer("q3", "c"),
                answer("q4", "d"),
                answer("q5", "e")));
        return request;
    }

    private AnswerRequest answer(String questionCode, String optionCode) {
        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionCode(questionCode);
        answer.setOptionCode(optionCode);
        return answer;
    }

    private ElementScoreResult buildScoreResult() {
        ElementScoreResult scoreResult = new ElementScoreResult();
        scoreResult.setPrimaryElement(ElementType.WOOD);
        scoreResult.setSecondaryElement(ElementType.FIRE);
        scoreResult.setPrimaryPercent(62);
        scoreResult.setSecondaryPercent(38);
        scoreResult.setAllScores(Map.of("WOOD", 62, "FIRE", 38, "EARTH", 20, "METAL", 18, "WATER", 16));
        return scoreResult;
    }

    private ResultText buildResultText() {
        ResultText resultText = new ResultText();
        resultText.setKeywords(List.of("成长", "表达"));
        resultText.setLayoutExplanation("木火相生");
        resultText.setStrengthText("适合持续推进");
        resultText.setRelationshipText("表达清晰");
        resultText.setPersonaTypeId("WOOD-FIRE-EARTH-dominant");
        resultText.setAccentElement("EARTH");
        resultText.setAccentElementName("土");
        resultText.setRelationKind("dominant");
        resultText.setPersonaLabel("贪狼成垣");
        resultText.setStarToneName("贪狼成垣");
        resultText.setStarToneLabel("星曜取象");
        resultText.setStructureTitle("贪狼成垣：木气开枝，火光照路");
        resultText.setHeroSummary("你会先用生长和规划回应世界，再借目标把反应整理成形；一方台基藏在关键处，让这份气质多出一点辨识度。");
        resultText.setIdentityLine("生长力 · 点亮感 · 承接感");
        resultText.setStarToneExplanation("「贪狼」取生长、探索、表现之象；「成垣」取结构、承载和稳定感成形。");
        resultText.setDayMasterText("日主依据：木火相生");
        resultText.setPrimarySecondaryText("主从关系：木火相生");
        resultText.setAccentText("点睛元素是土");
        resultText.setHeavenText("天：内在持续生长");
        resultText.setHumanText("人：外部表达清晰");
        resultText.setStarOfficerText("星宿锚点：井宿");
        resultText.setGrowthAdvice(List.of(
                new GrowthAdvice("先抓主线", "先推进最重要的一件事"),
                new GrowthAdvice("校准节奏", "让副元素参与判断"),
                new GrowthAdvice("点睛启动", "用土的稳定感开路"),
                new GrowthAdvice("固定复盘", "把感觉落成节奏")));
        return resultText;
    }

    private UserResultEntity buildStoredEntity() {
        UserResultEntity entity = new UserResultEntity();
        entity.setResultId("R-old");
        entity.setPrimaryElement("WATER");
        entity.setSecondaryElement("EARTH");
        entity.setPrimaryPercent(66);
        entity.setSecondaryPercent(34);
        entity.setAllElementScoresJson("{\"WATER\":66,\"EARTH\":34}");
        entity.setPersonaTypeId("WATER-EARTH-FIRE-dominant");
        entity.setAccentElement("FIRE");
        entity.setRelationKind("dominant");
        entity.setDayMasterText("日主依据：旧数据");
        entity.setPrimarySecondaryText("水土关系");
        entity.setAccentText("火点睛");
        entity.setHeavenText("内在");
        entity.setHumanText("外部");
        entity.setStarOfficerText("星官说明");
        entity.setGrowthAdviceJson("[]");
        entity.setStarOfficerCode("XU_XIU");
        entity.setStarOfficerName("虚宿");
        entity.setKeywordsJson("[]");
        entity.setLayoutExplanation("旧说明");
        entity.setStrengthText("旧强弱");
        entity.setRelationshipText("旧关系");
        return entity;
    }

    private UserResultEntity storedEntityWithLabel(String resultId, String label) {
        UserResultEntity entity = buildStoredEntity();
        entity.setResultId(resultId);
        entity.setPersonaLabel(label);
        return entity;
    }

    private ShortLinkEntity buildShortLink() {
        ShortLinkEntity shortLink = new ShortLinkEntity();
        shortLink.setShortCode("abc123");
        shortLink.setShortUrl("http://example.test/s/abc123");
        return shortLink;
    }
}
